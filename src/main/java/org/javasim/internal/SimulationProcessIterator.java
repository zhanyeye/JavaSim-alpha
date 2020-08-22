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

package org.javasim.internal;

import org.javasim.SimulationProcess;

public class SimulationProcessIterator {

    /**
     * 迭代器的游标
     */
    private SimulationProcessNode iterator;

    public SimulationProcessIterator(SimulationProcessList L) {
        iterator = L.Head;
    }


    /**
     * 返回当前迭代节点的进程，同时迭代游标后移
     * @return 当前迭代节点的进程
     */
    public final synchronized SimulationProcess get() {
        if (iterator != null) {
            SimulationProcessNode tmp = iterator;
            iterator = iterator.getNext();
            return tmp.getProcess();
        }
        return null;
    }



};

