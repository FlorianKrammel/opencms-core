/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/directedit/CmsDirectEditButtonSelection.java,v $
 * Date   : $Date: 2007/07/04 16:57:23 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.editors.directedit;

/**
 * Constants to indicate which direct edit buttons should be displayed for a direct edit resource
 * if the user has the permissions.<p>
 * 
 * The actual permission check is done later using {@link CmsDirectEditPermissions}.<p>
 * 
 * This button selection is used internally to indicate the buttons that <i>may</i> be displayed.
 * Usually, for an XmlPage only the "edit" button is displayed, while for an XmlContent
 * there may be an "edit", "delete" or "new" button.<p> 
 * 
 * Currently there are only constants for thouse button combinations that are actually used
 * in practice. These are {@link #EDIT}, {@link #EDIT_DELETE} and {@link #EDIT_DELETE_NEW}.<p>
 * 
 * @author Alexander Kandzior
 * 
 * @version $Revision: 1.3 $ 
 * 
 * @since 6.2.3
 */
public final class CmsDirectEditButtonSelection {

    /** Constant to indicate "show only the edit button". */
    public static final CmsDirectEditButtonSelection EDIT = new CmsDirectEditButtonSelection(true, false, false);

    /** Constant to indicate "show the edit and the delete button". */
    public static final CmsDirectEditButtonSelection EDIT_DELETE = new CmsDirectEditButtonSelection(true, true, false);

    /** Constant to indicate "show the edit, the delete and the new button". */
    public static final CmsDirectEditButtonSelection EDIT_DELETE_NEW = new CmsDirectEditButtonSelection(
        true,
        true,
        true);

    /** String value that indicates the "delete" button should be displayed. */
    public static final String VALUE_DELETE = "delete";

    /** String value that indicates the "edit" button should be displayed. */
    public static final String VALUE_EDIT = "edit";

    /** String value that indicates the "new" button should be displayed. */
    public static final String VALUE_NEW = "new";

    /** Indicates if the "delete" button should be displayed. */
    private boolean m_showDelete;

    /** Indicates if the "edit" button should be displayed. */
    private boolean m_showEdit;

    /** Indicates if the "new" button should be displayed. */
    private boolean m_showNew;

    /** Pre-calculated String value. */
    private String m_stringValue;

    /**
     * Hides the public constructor.<p> 
     * 
     * @param showEdit if <code>true</code> then the "edit" button should be displayed
     * @param showDelete if <code>true</code> then the "delete" button should be displayed
     * @param showNew if <code>true</code> then the "new" button should be displayed
     */
    private CmsDirectEditButtonSelection(boolean showEdit, boolean showDelete, boolean showNew) {

        m_showEdit = showEdit;
        m_showDelete = showDelete;
        m_showNew = showNew;
    }

    /**
     * Returns <code>true</code> if the "delete" button should be displayed.<p>
     *
     * @return <code>true</code> if the "delete" button should be displayed
     */
    public boolean isShowDelete() {

        return m_showDelete;
    }

    /**
     * Returns <code>true</code> if the "edit" button should be displayed.<p>
     *
     * @return <code>true</code> if the "edit" button should be displayed
     */
    public boolean isShowEdit() {

        return m_showEdit;
    }

    /**
     * Returns <code>true</code> if the "new" button should be displayed.<p>
     *
     * @return <code>true</code> if the "new" button should be displayed
     */
    public boolean isShowNew() {

        return m_showNew;
    }

    /**
     * Returns the selected edit options as a String in the form <code>edit|delete|new</code>.<p>
     * 
     * @return the selected edit options as a String
     */
    public String toString() {

        if (m_stringValue == null) {
            StringBuffer result = new StringBuffer(32);
            if (m_showEdit) {
                result.append(VALUE_EDIT);
            }
            result.append('|');
            if (m_showDelete) {
                result.append(VALUE_DELETE);
            }
            result.append('|');
            if (m_showNew) {
                result.append(VALUE_NEW);
            }
            m_stringValue = result.toString();
        }
        return m_stringValue;
    }
}