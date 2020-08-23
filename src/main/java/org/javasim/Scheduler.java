/*
 * Copyright 1990-2008, Mark Little, University of Newcastle upon Tyne
 * and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 1990-2008,
 */

/*
 * Copyright (C) 1996, 1997, 1998,
 *
 * Department of Computing Science,
 * The University,
 * Newcastle upon Tyne,
 * UK.
 *
 * $Id: Scheduler.java,v 1.3 1998/12/07 08:28:10 nmcl Exp $
 */

package org.javasim;

import java.util.NoSuchElementException;

import org.javasim.internal.SimulationProcessIterator;
import org.javasim.internal.SimulationProcessList;
import org.javasim.internal.SimulationProcessQueue;

/**
 * This is the scheduler: the heart of the simulation system.
 * 调度器，调度系统的核心
 * Note: unlike in SIMULA, an active process is removed from the simulation
 * queue prior to being activated.
 * 活动进程在被激活之前会从模拟队列中删除
 *
 * @author marklittle
 */
public class Scheduler extends Thread {

    /**
     * 系统模拟的仿真时间
     */
    private static double SimulatedTime = 0.0;
    /**
     * 事件调度队列
     */
    private static SimulationProcessQueue ReadyQueue = new SimulationProcessQueue();
    /**
     * 互斥锁
     */
    static Scheduler theScheduler = new Scheduler();

    private Scheduler() {
    }

    /**
     * Get the current simulation time.
     * 获取当前的仿真时间
     * @return the current simulation time.
     */
    public static double currentTime() {
        return Scheduler.SimulatedTime;
    }

    /**
     * This routine resets the simulation time to zero and removes all
     * entries from the scheduler queue (as their times may no longer
     * be valid). Whatever operation caused the processes to become
     * suspended will raise the RestartSimulation exception, which the
     * application should catch. It should then perform any work necessary
     * to put the process back in a state ready for restarting the simulation
     * before calling Cancel on the process.
     *
     * 这个例程将模拟时间重置为零，并从调度器队列中删除所有条目(因为它们的时间可能不再有效)。
     * 任何导致进程被挂起的操作都将引发RestartSimulation异常，应用程序应该捕获该异常。
     * 然后，在对进程调用Cancel之前，它应该执行任何必要的工作，将进程恢复到可以重新启动模拟的状态。
     * @throws SimulationException if an error occurs.
     */
    static synchronized void reset() throws SimulationException {
        boolean finished = false;
        SimulationProcess tmp = SimulationProcess.current();

        // set resetting process to idle

        // 将激活的进程切换为 passive
        Scheduler.unschedule(tmp);

        // 将挂起的进程状态切换成 passive
        do {
            try {
                tmp = Scheduler.ReadyQueue.remove();
                tmp.deactivate();
            } catch (NoSuchElementException e) {
                finished = true;
            }

        } while (!finished);

        finished = false;

        // 仿真系统中的非 terminal 线程
        SimulationProcessQueue allProcesses = SimulationProcess.allProcesses;
        SimulationProcess iterator = allProcesses.peek();

        /**
         * todo 是否正确有待验证
         */

        do {
            try {
                tmp = iterator;

                /*
                 * Every process must be in Suspend, so we call Resume
                 * and get each one to check whether the simulation is
                 * restarting. If it is, it raises an exception and waits
                 * for the user to cancel the process after setting it
                 * to become ready to restart.
                 */

                tmp.resumeProcess();

                /*
                 * Wait for this process to become idle again.
                 */

                while (!tmp.idle())
                    Thread.yield();
            } catch (NullPointerException e) {
                finished = true;
            }

            iterator = allProcesses.getNext(iterator);

        } while (!finished);

        Scheduler.SimulatedTime = 0.0;

        SimulationProcess.Current = null;
        SimulationProcess.allProcesses = new SimulationProcessQueue();
    }

    /**
     * It is possible that the currently active process may remove itself
     * from the simulation queue. In which case we don't want to suspend the
     * process since it needs to continue to run. The return value indicates
     * whether or not to call suspend on the currently active process.
     *
     * 调度器从进程队列中调度一个进程
     */
    static synchronized boolean schedule() throws SimulationException {
        if (Simulation.isStarted()) {
            SimulationProcess p = SimulationProcess.current();

            try {
                /*
                 * For some reason when executing tests in junit an old and dead
                 * thread appears in the simulation queue. Have only ever seen this
                 * be a single thread instance, but it is reproducible every time.
                 *
                 * https://github.com/nmcl/JavaSim/issues/64
                 *
                 * Will try to find out what actually causes this and remove the
                 * workaround eventually.
                 *
                 * https://github.com/nmcl/JavaSim/issues/76
                 */

                SimulationProcess.Current = Scheduler.ReadyQueue.remove();
                boolean done = true;

                do {
                    if (SimulationProcess.Current != null) {
                        if (SimulationProcess.Current.getThreadGroup() == null) {
                            // 如果此线程已死(已停止)
                            SimulationProcess.Current = Scheduler.ReadyQueue.remove();
                            p = SimulationProcess.current();
                            done = false;
                        } else {
                            done = true;
                        }
                    } else {
                        throw new NoSuchElementException();
                    }
                }
                while (!done);
            } catch (NoSuchElementException e) {
                System.out.println("Simulation queue empty.");

                return false;
            } catch (NullPointerException e) {
                System.out.println("Simulation queue empty.");

                return false;
            }

            if (SimulationProcess.Current.evtime() < 0) {
                throw new SimulationException("Invalid SimulationProcess wakeup time.");
            } else {
                // 系统仿真时间推进
                Scheduler.SimulatedTime = SimulationProcess.Current.evtime();
            }

            if (p != SimulationProcess.Current) {
                // Simulation.printQueue();

                SimulationProcess.Current.resumeProcess();
                return true;
            } else {
                return false;
            }
        } else {
            throw new SimulationException("Simulation not started.");
        }
    }

    /**
     * 将指定线程从调度队列中移除，并将线程状态切换为空闲
     * @param process
     */
    static synchronized void unschedule(SimulationProcess process) {
        try {
            // 将线程从队列中移除
            Scheduler.ReadyQueue.remove(process);
        } catch (NoSuchElementException e) {
        }
        // 切换线程状态为空闲
        process.deactivate();
    }

    /**
     * 获得事件队列
     * @return
     */
    static SimulationProcessQueue getQueue() {
        synchronized (theScheduler) {
            return ReadyQueue;
        }
    }

    /**
     * 获得系统仿真时间
     * @return
     */
    static double getSimulationTime() {
        synchronized (theScheduler) {
            return SimulatedTime;
        }
    }

}
