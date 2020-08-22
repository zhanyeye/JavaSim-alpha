package org.javasim.internal;

import org.javasim.SimulationProcess;

/**
 * @description:
 * @author: zhanyeye
 * @create: 2020-08-18 12:29
 **/
class SimulationProcessNode {

    /**
     * 存放的进程
     */
    private SimulationProcess process;

    /**
     * 它的后继节点
     */
    private SimulationProcessNode next;


    public SimulationProcessNode(SimulationProcess process, SimulationProcessNode node) {
        this.process = process;
        next = node;
    }

    /**
     * 获取节点中的线程
     * @return
     */
    public final SimulationProcess getProcess() {
        return process;
    }

    /**
     * 获取该节点的后继
     * @return
     */
    public final SimulationProcessNode getNext() {
        return next;
    }

    /**
     * 更新该节点的后继
     * @param next 后继节点
     */
    public final void setNext(SimulationProcessNode next) {
        this.next = next;
    }

}
