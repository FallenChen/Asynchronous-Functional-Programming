/*
 * Copyright 2010 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL. 
 * All other files are covered by the Common Public License (CPL). 
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki
package kernel
package component

import org.agilewiki.kernel.element.Element
import util.jit.{JitNamedJitTreeMap, JitNamedVariableJitTreeMap}

trait TreeMapContainerComponent extends ContainerComponent {
  this: Element =>

  override protected def defineContents = new TreeMapContainer

  override def contents: TreeMapContainer = _contents.asInstanceOf[TreeMapContainer]

  class TreeMapContainer extends Container {
    private var _embeddedElementsContainer: JitNamedJitTreeMap = _

    override def containerWrapper = embeddedElementsContainer

    protected def createEmbeddedElementsContainer: JitNamedJitTreeMap =
      JitNamedVariableJitTreeMap.createJit(systemContext)

    override def embeddedElementsContainer = {
      deserialize
      _embeddedElementsContainer
    }

    override def builder = {
      _embeddedElementsContainer = createEmbeddedElementsContainer
      _embeddedElementsContainer.partness(TreeMapContainerComponent.this, null, TreeMapContainerComponent.this)
      _embeddedElementsContainer.jitByteLength
    }
  }

}
