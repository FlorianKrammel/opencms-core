/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/explorer/menu/CmsMirDirectPublish.java,v $
 * Date   : $Date: 2007/07/04 16:56:53 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.explorer.menu;

import org.opencms.file.CmsObject;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.security.CmsPermissionSet;
import org.opencms.workplace.explorer.CmsResourceUtil;

/**
 * Defines a menu item rule that sets the visibility to active
 * if the current resource can be directly published by the current user.<p>
 * 
 * @author Andreas Zahner  
 * 
 * @version $Revision: 1.2 $ 
 * 
 * @since 6.5.6
 */
public class CmsMirDirectPublish extends A_CmsMenuItemRule {

    /**
     * @see org.opencms.workplace.explorer.menu.I_CmsMenuItemRule#getVisibility(org.opencms.file.CmsObject, CmsResourceUtil[])
     */
    public CmsMenuItemVisibilityMode getVisibility(CmsObject cms, CmsResourceUtil[] resourceUtil) {

        CmsLock lock = resourceUtil[0].getLock();
        if (lock.isNullLock()
            || (lock.isExclusiveOwnedInProjectBy(
                cms.getRequestContext().currentUser(),
                cms.getRequestContext().currentProject()))) {
            // resource is not locked or exclusively locked by current user in current project

            try {
                if (cms.hasPermissions(resourceUtil[0].getResource(), CmsPermissionSet.ACCESS_DIRECT_PUBLISH)) {
                    // only activate if user has direct publish permissions
                    if (resourceUtil[0].getResource().isFolder()
                        || !resourceUtil[0].getResource().getState().isUnchanged()) {
                        // resource is a folder or not unchanged
                        return CmsMenuItemVisibilityMode.VISIBILITY_ACTIVE;
                    } else if (!resourceUtil[0].getResource().isFolder()
                        && resourceUtil[0].getResource().getState().isUnchanged()) {
                        return CmsMenuItemVisibilityMode.VISIBILITY_INACTIVE;
                    }
                }
            } catch (CmsException e) {
                // ignore, should not happen
            }
        }

        return CmsMenuItemVisibilityMode.VISIBILITY_INVISIBLE;
    }

    /**
     * @see org.opencms.workplace.explorer.menu.I_CmsMenuItemRule#matches(org.opencms.file.CmsObject, CmsResourceUtil[])
     */
    public boolean matches(CmsObject cms, CmsResourceUtil[] resourceUtil) {

        // rule does match if resource is part of the current project
        return resourceUtil[0].isInsideProject();
    }

}
