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
 * $Id: SimulationProcess.java,v 1.3 1998/12/07 08:28:11 nmcl Exp $
 */

package org.javasim;

import java.util.NoSuchElementException;

import org.javasim.internal.SimulationProcessList;
import org.javasim.internal.SimulationProcessQueue;

public class SimulationProcess extends Thread {
    public static final int NEVER = -1;

    /**
     * 仿真系统中的非 terminal 线程 （类变量）
     */
    static SimulationProcessQueue allProcesses = new SimulationProcessQueue();
    /**
     * 进程唤醒时间
     */
    private double wakeuptime;
    /**
     * 线程是否结束
     */
    private boolean terminated;
    /**
     * 线程是否空闲 ？？
     */
    private boolean passivated;
    /**
     *
     */
    private boolean started;
    /**
     * 互斥锁
     */
    private Object mutex = new Object();
    /**
     * 线程调度的信号量，1 表示可以挂起， 0 表示等待唤醒
     */
    private int count = 1;

    private static Thread mainThread = null;
    /**
     * 当前激活的线程 （类的属性）
     */
    static SimulationProcess Current = null;


    protected SimulationProcess() {
        wakeuptime = SimulationProcess.NEVER;
        terminated = false;
        passivated = true;
        started = false;

        SimulationProcess.allProcesses.insert(this);
    }

    public void finalize() {
        if (!terminated) {
            terminated = true;
            passivated = true;
            wakeuptime = SimulationProcess.NEVER;

            if (!idle()) {
                // remove from scheduler queue
                Scheduler.unschedule(this);
            }

            if (this == SimulationProcess.Current) {
                try {
                    Scheduler.schedule();
                } catch (SimulationException e) {
                }
            }

        }
    }

    /**
     * 获取当前的仿真时间
     *
     * @return the current simulation time.
     */
    public final double time() {
        return SimulationProcess.currentTime();
    }

    /**
     * 返回调度队列中某进程的后一个进程
     *
     * @return the next simulation process which will run.
     * @throws SimulationException    thrown if there's an error.
     * @throws NoSuchElementException thrown if there is no next processs.
     */
    public synchronized SimulationProcess nextEv() throws SimulationException, NoSuchElementException {
        if (!idle()) {
            // 该进程仍在等待队列中，没被唤醒
            return Scheduler.getQueue().getNext(this);
        } else {
            // 该进程已经被唤醒，空闲状态
            throw new SimulationException("SimulationProcess not on run queue.");
        }
    }

    /**
     * 返回进程的唤醒时间
     *
     * @return the simulation time at which this process will run.
     */
    public final double evtime() {
        return wakeuptime;
    }

    /**
     * 在进程 p 之前激活此进程
     * 该进程不能是正是运行或者已经在事件队列中
     * Activate this process before process 'p'. This process must not be
     * running, or on the scheduler queue.
     *
     * @param p the 'before' process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateBefore(SimulationProcess p) throws SimulationException, RestartException {
        if (terminated || !idle()) {
            return;
        }

        passivated = false;

        if (Scheduler.getQueue().insertBefore(this, p)) {
            wakeuptime = p.wakeuptime;
        } else {
            throw new SimulationException("'before' process is not scheduled.");
        }
    }

    /**
     * 在进程 p 之后激活此进程
     * 该进程不能是正是运行或者已经在事件队列中
     * Activate this process after process 'p'. This process must not be
     * running, or on the scheduler queue.
     *
     * @param p the 'after' process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateAfter(SimulationProcess p) throws SimulationException, RestartException {
        if (p == this) {
            throw new SimulationException("'after' cannot be identical to self.");
        }

        if (terminated || !idle()) {
            return;
        }

        passivated = false;

        if (Scheduler.getQueue().insertAfter(this, p)) {
            wakeuptime = p.wakeuptime;
        } else {
            throw new SimulationException("'after' process is not scheduled.");
        }
    }

    /**
     * 激活该进程在指定时间
     * 该进程不能是正是运行或者已经在事件队列中
     * Activate this process at the specified simulation time. This process must
     * not be running, or on the scheduler queue. 'AtTime' must be greater than,
     * or equal to, the current simulation time. If 'prior' is true then this
     * process will appear in the simulation queue before any other process with
     * the same simulation time.
     *
     * @param AtTime the time to activate the process.
     * @param prior  indicates whether or not to schedule this process occurs before any other process with the same time.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateAt(double AtTime, boolean prior) throws SimulationException, RestartException {
        if (terminated || !idle()) {
            return;
        }

        if (AtTime < SimulationProcess.currentTime()) {
            throw new SimulationException("Invalid time " + AtTime);
        }

        passivated = false;
        wakeuptime = AtTime;
        Scheduler.getQueue().insert(this, prior);
    }

    /**
     * 激活该进程在指定时间
     * 该进程不能是正是运行或者已经在事件队列中
     * Activate this process at the specified simulation time. This process must
     * not be running, or on the scheduler queue. 'AtTime' must be greater than,
     * or equal to, the current simulation time.
     *
     * @param AtTime the time to activate this process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateAt(double AtTime) throws SimulationException, RestartException {
        activateAt(AtTime, false);
    }

    /**
     * 进程将在指定仿真时间后被激活
     * This process will be activated after 'Delay' units of simulation time.
     * This process must not be running, or on the scheduler queue. 'Delay' must
     * be greater than, or equal to, zero. If 'prior' is true then this process
     * will appear in the simulation queue before any other process with the
     * same simulation time.
     *
     * @param Delay the time by which to delay this process.
     * @param prior indicates whether or not to schedule this process occurs before any other process with the same time.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateDelay(double Delay, boolean prior) throws SimulationException, RestartException {
        if (terminated || !idle()) {
            return;
        }

        if (Delay < 0) {
            throw new SimulationException("Invalid delay time " + Delay);
        }

        passivated = false;
        wakeuptime = Scheduler.getSimulationTime() + Delay;
        Scheduler.getQueue().insert(this, prior);
    }

    /**
     * 进程将在指定仿真时间后被激活
     * This process will be activated after 'Delay' units of simulation time.
     * This process must not be running, or on the scheduler queue. 'Delay' must
     * be greater than, or equal to, zero.
     *
     * @param Delay the time by which to delay this process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activateDelay(double Delay) throws SimulationException, RestartException {
        activateDelay(Delay, false);
    }

    /**
     * 在系统当前仿真时间激活
     * Activate this process at the current simulation time. This process must
     * not be running, or on the scheduler queue.
     *
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void activate() throws SimulationException, RestartException {
        if (terminated || !idle()) {
            return;
        }

        passivated = false;
        wakeuptime = currentTime();
        Scheduler.getQueue().insert(this, true);
    }

    /**
     * 重新激活进程，在 p 之前
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process before process 'p'.
     *
     * @param p the process to reactivate this process before.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */

    public void reactivateBefore(SimulationProcess p) throws SimulationException, RestartException {
        if (!idle()) {
            // 若进程正在运行，或还在等待队列中
            // 则将该进程从队列中移除
            Scheduler.unschedule(this);
        }

        activateBefore(p);

        if (SimulationProcess.Current == this) {
            suspendProcess();
        }
    }

    /**
     * 重新激活进程，在 p 之前
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process after process 'p'.
     *
     * @param p the process to reactivate this process after.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void reactivateAfter(SimulationProcess p)
            throws SimulationException, RestartException {
        if (!idle()) {
            Scheduler.unschedule(this);
        }

        activateAfter(p);

        if (SimulationProcess.Current == this) {
            suspendProcess();
        }
    }

    /**
     * 重新激活进程，在指定仿真时间
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process at the specified simulation time. 'AtTime' must
     * be valid. If 'prior' is true then this process will appear in the
     * simulation queue before any other process with the same simulation time.
     *
     * @param AtTime the time at which to reactivate this process.
     * @param prior  indicates whether or not to schedule this process occurs before any other process with the same time.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */

    public void reactivateAt(double AtTime, boolean prior)
            throws SimulationException, RestartException {
        if (!idle()) {
            Scheduler.unschedule(this);
        }

        activateAt(AtTime, prior);

        if (SimulationProcess.Current == this) {
            suspendProcess();
        }
    }

    /**
     * 重新激活进程在 指定仿真时间
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process at the specified simulation time. 'AtTime' must
     * be valid.
     *
     * @param AtTime the time at which to reactivate this process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void reactivateAt(double AtTime) throws SimulationException, RestartException {
        reactivateAt(AtTime, false);
    }

    /**
     * 重新激活进程在指定时间之后
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process after 'Delay' units of simulation time. If
     * 'prior' is true then this process will appear in the simulation queue
     * before any other process with the same simulation time.
     *
     * @param Delay the time to delay this process by before reactivation.
     * @param prior prior indicates whether or not to schedule this process occurs before any other process with the same time.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void reactivateDelay(double Delay, boolean prior) throws SimulationException, RestartException {
        if (!idle()) {
            Scheduler.unschedule(this);
        }

        activateDelay(Delay, prior);

        if (SimulationProcess.Current == this) {
            suspendProcess();
        }
    }

    /**
     * 重新激活进程在指定时间之后
     * 若当前线程不是空闲，则先从队列移除，若当前线程正在运行则挂起
     * Reactivate this process after 'Delay' units of simulation time.
     *
     * @param Delay the time to delay this process.
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void reactivateDelay(double Delay) throws SimulationException, RestartException {
        reactivateDelay(Delay, false);
    }

    /**
     * 重新激活线程在当前仿真时间
     * Reactivate this process at the current simulation time.
     *
     * @throws SimulationException thrown if there's an error.
     * @throws RestartException    thrown if the simulation is restarted.
     */
    public void reactivate() throws SimulationException, RestartException {
        if (!idle()) {
            Scheduler.unschedule(this);
        }

        activate();

        if (SimulationProcess.Current == this) {
            suspendProcess();
        }
    }

    /**
     * Cancels next burst of activity, process becomes idle.
     *
     * @throws RestartException thrown if the simulation is restarted.
     */
    public void cancel() throws RestartException {
        /*
         * We must suspend this process either by removing it from the scheduler
         * queue (if it is already suspended) or by calling suspend directly.
         */

        if (!idle()) {
            // process is running or on queue to be run
            if (this == SimulationProcess.Current) {
                // currently active, so simply suspend
                wakeuptime = SimulationProcess.NEVER;
                passivated = true;
                suspendProcess();
            } else {
                Scheduler.unschedule(this); // remove from queue
            }
        }
    }

    /**
     * Terminate this process: no going back!
     */
    public void terminate() {
        if (!terminated) {
            terminated = passivated = true;
            wakeuptime = SimulationProcess.NEVER;

            if ((this != SimulationProcess.Current) && (!idle())) {
                Scheduler.unschedule(this);
            }

            try {
                // 调度器继续下一个事件的调度
                Scheduler.schedule();
            } catch (SimulationException e) {
            }

            SimulationProcess.allProcesses.remove(this);
        }
    }

    /**
     * Is the process idle?
     * 进程是否空闲
     * 若该进程正在运行或在调度队列中，则返回 false
     *
     * @return whether or not this process is idle.
     */
    public synchronized boolean idle() {
        if (wakeuptime >= SimulationProcess.currentTime()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 该线程是否已经空闲
     * Has the process been passivated?
     *
     * @return whether or not this process is passive.
     */
    public boolean passivated() {
        return passivated;
    }

    /**
     * 线程是否已经结束
     * Has the process been terminated?
     *
     * @return whether or not this process has been terminated.
     */
    public boolean terminated() {
        return terminated;
    }

    /**
     * 返回当前正在运行的线程
     * @return the currently active simulation process.
     * @throws SimulationException thrown if there's an error.
     */
    public static SimulationProcess current() throws SimulationException {
        if (SimulationProcess.Current == null) {
            throw new SimulationException("Current not set.");
        }

        return SimulationProcess.Current;
    }

    /**
     * 获取仿真系统的当前模拟时间
     *
     * @return the current simulation time.
     */
    public static double currentTime() {
        return Scheduler.getSimulationTime();
    }

    /**
     * 用于挂起主线程
     * Suspend the main thread.
     */
    public static void mainSuspend() {
        SimulationProcess.mainThread = Thread.currentThread();

        synchronized (SimulationProcess.mainThread) {
            try {
                SimulationProcess.mainThread.wait();
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 恢复主线程
     * Resume the main thread.
     */

    public static void mainResume() throws SimulationException {
        if (SimulationProcess.mainThread == null) {
            throw new SimulationException("No main thread");
        }

        synchronized (SimulationProcess.mainThread) {
            try {
                SimulationProcess.mainThread.notify();
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * 设置事件发生时间
     * @param time
     * @throws SimulationException
     */
    protected void setEvtime(double time) throws SimulationException {
        if (!idle()) {
            if (time >= SimulationProcess.currentTime()) {
                wakeuptime = time;
            } else {
                throw new SimulationException("Time " + time + " invalid.");
            }
        } else {
            throw new SimulationException("SimulationProcess is not idle.");
        }
    }

    /**
     * Hold the current process for the specified amount of simulation time.
     */
    protected void hold(double t) throws SimulationException, RestartException {
        if ((this == SimulationProcess.Current) || (SimulationProcess.Current == null)) {
            wakeuptime = SimulationProcess.NEVER;
            activateDelay(t, false);
            suspendProcess();
        } else {
            throw new SimulationException("Hold applied to inactive object.");
        }
    }

    /**
     * 使该线程空闲
     * @throws RestartException
     */
    protected void passivate() throws RestartException {
        if (!passivated && (this == SimulationProcess.Current)) {
            cancel();
        }
    }

    /**
     * 挂起该线程等待唤醒
     * Suspend the process. If it is not running, then this routine should not
     * be called.
     */
    protected void suspendProcess() throws RestartException {
        try {
            if (Scheduler.schedule()) {
                synchronized (mutex) {
                    count--;

                    if (count == 0) {
                        try {
                            mutex.wait();
                        } catch (Exception e) {
                        }
                    }

                }
            }
        } catch (SimulationException e) {
        }

        // 判断线程是否正被重置
        if (Simulation.isReset()) {
            throw new RestartException();
        }
    }

    /**
     * 恢复该线程
     * 只能被刚创建的/之前被挂起的线程调用
     * Resume the specified process. This can only be called on a process which
     * has previously been Suspend-ed or has just been created, i.e., the
     * currently active process will never have Resume called on it.
     */
    protected void resumeProcess() {
        /*
         * To compensate for the initial call to Resume by the application.
         */

        if (SimulationProcess.Current == null) {
            SimulationProcess.Current = this;
            wakeuptime = SimulationProcess.currentTime();
        }

        if (!terminated) {
            if (!started) {
                started = true;
                start();
            } else {
                synchronized (mutex) {
                    count++;

                    if (count >= 0) {
                        mutex.notify();
                    }
                }
            }
        }
    }


    /**
     * 切换线程到 空闲 状态
     * passive: the process is not on the scheduler queue. Unless another process brings it back on to the queue it will not execute any further actions.
     */
    void deactivate() {
        passivated = true;
        wakeuptime = SimulationProcess.NEVER;
    }

}
