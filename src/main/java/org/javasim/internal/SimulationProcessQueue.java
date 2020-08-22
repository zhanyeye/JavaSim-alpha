package org.javasim.internal;

import org.javasim.SimulationProcess;

import java.util.*;

/**
 * @description: 事件队列
 * @author: zhanyeye
 * @create: 2020-08-18 12:03
 **/
public class SimulationProcessQueue {

    /**
     * 仿真事件队列
     */
    private List<SimulationProcess> queue = new LinkedList<>();

    /**
     * 互斥锁对象
     */
    private Object mutex = new Object();

    /**
     * 向事件队列插入线程，不考虑优先级
     * @param process
     */
    public void insert(SimulationProcess process) {
        insert(process, false);
    }

    /**
     * 向事件队列插入线程，考虑优先级
     * @param toInsert 需要插入的线程
     * @param prior 是否插在目标前
     */
    public void insert(SimulationProcess toInsert, boolean prior) {
        synchronized (mutex) {
            if (queue.isEmpty()) {
                queue.add(toInsert);
                return;
            }
            for (SimulationProcess process : queue) {
                if (prior) {
                    if (toInsert.evtime() <= process.evtime()) {
                        insertBefore(toInsert, process);
                        return;
                    }
                } else {
                    if (toInsert.evtime() < process.evtime()) {
                        insertBefore(toInsert, process);
                        return;
                    }
                }
            }
            // 插入队列尾部
            queue.add(toInsert);
        }
    }

    /**
     * 插入到指定进程之前
     * @param toInsert 需要插入的线程
     * @param target 目标线程
     * @return 如果插入的位置存在，返回true
     */
    public boolean insertBefore(SimulationProcess toInsert, SimulationProcess target) {
        synchronized (mutex) {
            int index = queue.indexOf(target);
            if (index > -1) {
                queue.add(index, toInsert);
                return true;
            }
            return false;
        }
    }

    /**
     *
     * @param toInsert 需要插入的线程
     * @param target 目标线程
     * @return 如果插入的位置存在，返回true
     */
    public boolean insertAfter(SimulationProcess toInsert, SimulationProcess target) {
        synchronized (mutex) {
            int index = queue.indexOf(target);
            if (index > -1) {
                queue.add(index + 1, toInsert);
                return true;
            }
            return false;
        }
    }


    /**
     * 移除事件队列中的某一个线程，并返回该线程
     * @param target 需要删除的线程
     * @return 被删除的线程
     * @throws NoSuchElementException 如果事件队列中没有要删除的目标
     */
    public SimulationProcess remove(SimulationProcess target) throws NoSuchElementException {
        synchronized (mutex) {
            if(queue.isEmpty()) {
                throw new NoSuchElementException();
            }
            boolean isSuccess = queue.remove(target);
            if (!isSuccess) {
                throw new NoSuchElementException();
            }
            return target;
        }
    }

    /**
     * 弹出队首线程
     * @return 被弹出的元素
     * @throws NoSuchElementException 队列为空
     */
    public SimulationProcess remove() throws NoSuchElementException {
        synchronized (mutex) {
            if (queue.isEmpty()) {
                throw new NoSuchElementException();
            }
            return queue.remove(0);
        }
    }

    /**
     * 获得current线程的后继线程
     * @param current 基准线程
     * @return current线程的后继
     * @throws NoSuchElementException 队列或者current为空
     */
    public SimulationProcess getNext(SimulationProcess current) throws NoSuchElementException {
        synchronized (mutex) {
            if (queue.isEmpty() || (current == null)) {
                throw new NoSuchElementException();
            }
            // todo 这里可以根据事件发生时间优化
            int index = queue.indexOf(current);
            if (index == queue.size() -1) {
                // current是队列的最后一个元素
                return null;
            } else if (index != -1){
                // 队列中存在current
                return queue.get(index + 1);
            } else {
                // 队列中没有current,意味着current处于活动状态
                return queue.get(0);
            }
        }
    }

    /**
     * 打印事件发生的时间
     */
    public void print() {
        synchronized (mutex) {
            for(SimulationProcess process : queue) {
                System.out.println(process.evtime());
            }
        }
    }

}
