/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/accounts/A_CmsGroupUsersList.java,v $
 * Date   : $Date: 2007/07/04 16:56:43 $
 * Version: $Revision: 1.17 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.workplace.tools.accounts;

import org.opencms.file.CmsUser;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.security.CmsOrganizationalUnit;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.list.A_CmsListDialog;
import org.opencms.workplace.list.CmsListColumnAlignEnum;
import org.opencms.workplace.list.CmsListColumnDefinition;
import org.opencms.workplace.list.CmsListIndependentAction;
import org.opencms.workplace.list.CmsListItem;
import org.opencms.workplace.list.CmsListItemDetails;
import org.opencms.workplace.list.CmsListItemDetailsFormatter;
import org.opencms.workplace.list.CmsListMetadata;
import org.opencms.workplace.list.CmsListOrderEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;

/**
 * Generalized user groups view.<p>
 * 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.17 $ 
 * 
 * @since 6.0.0 
 */
public abstract class A_CmsGroupUsersList extends A_CmsListDialog {

    /** list action id constant. */
    public static final String LIST_ACTION_ICON = "ai";

    /** list action id constant. */
    public static final String LIST_ACTION_STATE = "as";

    /** list column id constant. */
    public static final String LIST_COLUMN_FULLNAME = "cf";

    /** list column id constant. */
    public static final String LIST_COLUMN_ICON = "ci";

    /** list column id constant. */
    public static final String LIST_COLUMN_LOGIN = "cl";

    /** list column id constant. */
    public static final String LIST_COLUMN_NAME = "cn";

    /** list column id constant. */
    public static final String LIST_COLUMN_ORGUNIT = "co";

    /** list column id constant. */
    public static final String LIST_COLUMN_STATE = "cs";

    /** list item detail id constant. */
    public static final String LIST_DETAIL_OTHEROU = "doo";

    /** Cached value. */
    private Boolean m_hasUsersInOtherOus;

    /** Stores the value of the request parameter for the user id. */
    private String m_paramGroupid;

    /** Stores the value of the request parameter for the user name. */
    private String m_paramGroupname;

    /** Stores the value of the request parameter for the organizational unit fqn. */
    private String m_paramOufqn;

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     * @param listId the id of the list
     * @param listName the name of the list
     * @param searchable searchable flag
     */
    protected A_CmsGroupUsersList(
        CmsJspActionElement jsp,
        String listId,
        CmsMessageContainer listName,
        boolean searchable) {

        super(jsp, listId, listName, LIST_COLUMN_LOGIN, CmsListOrderEnum.ORDER_ASCENDING, searchable ? LIST_COLUMN_NAME
        : null);
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#actionDialog()
     */
    public void actionDialog() throws JspException, ServletException, IOException {

        updateGroupList();
        super.actionDialog();
    }

    /**
     * Returns the user id parameter value.<p>
     * 
     * @return the user id parameter value
     */
    public String getParamGroupid() {

        return m_paramGroupid;
    }

    /**
     * Returns the Group name parameter.<p>
     *
     * @return the Group name paramter
     */
    public String getParamGroupname() {

        return m_paramGroupname;
    }

    /**
     * Returns the right icon path for the given list item.<p>
     * 
     * @param item the list item to get the icon path for
     * 
     * @return the icon path for the given role
     */
    public String getIconPath(CmsListItem item) {

        try {
            CmsUser user = getCms().readUser((String)item.get(LIST_COLUMN_LOGIN));
            if (user.getOuFqn().equals(getParamOufqn())) {
                return A_CmsUsersList.PATH_BUTTONS + "user.png";
            } else {
                return A_CmsUsersList.PATH_BUTTONS + "user_other_ou.png";
            }
        } catch (CmsException e) {
            return A_CmsUsersList.PATH_BUTTONS + "user.png";
        }
    }

    /**
     * Returns the organizational unit fqn parameter value.<p>
     * 
     * @return the organizational unit fqn parameter value
     */
    public String getParamOufqn() {

        return m_paramOufqn;
    }

    /**
     * Returns true if the list of users has users of other organizational units.<p>
     * 
     * @return <code>true</code> if the list of users has users of other organizational units
     */
    public boolean hasUsersInOtherOus() {

        if (m_hasUsersInOtherOus == null) {
            // lazzy initialization
            m_hasUsersInOtherOus = Boolean.FALSE;
            try {
                Iterator itUsers = getUsers(true).iterator();
                while (itUsers.hasNext()) {
                    CmsUser user = (CmsUser)itUsers.next();
                    if (!user.getOuFqn().equals(getParamOufqn())) {
                        m_hasUsersInOtherOus = Boolean.TRUE;
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return m_hasUsersInOtherOus.booleanValue();
    }

    /**
     * Sets the user id parameter value.<p>
     * 
     * @param userId the user id parameter value
     */
    public void setParamGroupid(String userId) {

        m_paramGroupid = userId;
    }

    /**
     * Sets the organizational unit fqn parameter value.<p>
     * 
     * @param ouFqn the organizational unit fqn parameter value
     */
    public void setParamOufqn(String ouFqn) {

        if (ouFqn == null) {
            ouFqn = "";
        }
        m_paramOufqn = ouFqn;
    }

    /**
     * Updates the main user list.<p>
     */
    public void updateGroupList() {

        Map objects = (Map)getSettings().getListObject();
        if (objects != null) {
            objects.remove(CmsGroupsList.class.getName());
            objects.remove(A_CmsUsersList.class.getName());
        }
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#fillDetails(java.lang.String)
     */
    protected void fillDetails(String detailId) {

        // noop
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#getListItems()
     */
    protected List getListItems() throws CmsException {

        List ret = new ArrayList();

        boolean withOtherOus = hasUsersInOtherOus()
            && getList().getMetadata().getItemDetailDefinition(LIST_DETAIL_OTHEROU).isVisible();

        // get content        
        Iterator itUsers = getUsers(withOtherOus).iterator();
        while (itUsers.hasNext()) {
            CmsUser user = (CmsUser)itUsers.next();
            CmsListItem item = getList().newItem(user.getId().toString());
            item.set(LIST_COLUMN_LOGIN, user.getName());
            item.set(LIST_COLUMN_NAME, user.getSimpleName());
            item.set(LIST_COLUMN_ORGUNIT, CmsOrganizationalUnit.SEPARATOR + user.getOuFqn());
            item.set(LIST_COLUMN_FULLNAME, user.getFullName());
            ret.add(item);
        }

        return ret;
    }

    /**
     * Returns a list of users to display.<p>
     * 
     * @param withOtherOus if not set only users of the current ou should be returned
     * 
     * @return a list of <code><{@link CmsUser}</code>s
     * 
     * @throws CmsException if something goes wrong
     */
    protected abstract List getUsers(boolean withOtherOus) throws CmsException;

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#initializeDetail(java.lang.String)
     */
    protected void initializeDetail(String detailId) {

        super.initializeDetail(detailId);
        if (detailId.equals(LIST_DETAIL_OTHEROU)) {
            boolean visible = hasUsersInOtherOus()
                && getList().getMetadata().getItemDetailDefinition(LIST_DETAIL_OTHEROU).isVisible();
            getList().getMetadata().getColumnDefinition(LIST_COLUMN_ORGUNIT).setVisible(visible);
            getList().getMetadata().getColumnDefinition(LIST_COLUMN_ORGUNIT).setPrintable(visible);
        }
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initMessages()
     */
    protected void initMessages() {

        // add specific dialog resource bundle
        addMessages(Messages.get().getBundleName());
        // add default resource bundles
        super.initMessages();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setColumns(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setColumns(CmsListMetadata metadata) {

        // create column for icon display
        CmsListColumnDefinition iconCol = new CmsListColumnDefinition(LIST_COLUMN_ICON);
        iconCol.setName(Messages.get().container(Messages.GUI_USERS_LIST_COLS_ICON_0));
        iconCol.setHelpText(Messages.get().container(Messages.GUI_USERS_LIST_COLS_ICON_HELP_0));
        iconCol.setWidth("20");
        iconCol.setAlign(CmsListColumnAlignEnum.ALIGN_CENTER);
        iconCol.setSorteable(false);
        // set icon action
        setIconAction(iconCol);
        // add it to the list definition
        metadata.addColumn(iconCol);

        setStateActionCol(metadata);

        // create column for login
        CmsListColumnDefinition loginCol = new CmsListColumnDefinition(LIST_COLUMN_LOGIN);
        loginCol.setVisible(false);
        // add it to the list definition
        metadata.addColumn(loginCol);

        // create column for name
        CmsListColumnDefinition nameCol = new CmsListColumnDefinition(LIST_COLUMN_NAME);
        nameCol.setName(Messages.get().container(Messages.GUI_USERS_LIST_COLS_LOGIN_0));
        nameCol.setWidth("35%");
        setDefaultAction(nameCol);
        // add it to the list definition
        metadata.addColumn(nameCol);

        // create column for orgunit
        CmsListColumnDefinition orgunitCol = new CmsListColumnDefinition(LIST_COLUMN_ORGUNIT);
        orgunitCol.setName(Messages.get().container(Messages.GUI_USERS_LIST_COLS_ORGUNIT_0));
        orgunitCol.setVisible(false);
        // add it to the list definition
        metadata.addColumn(orgunitCol);

        // create column for fullname
        CmsListColumnDefinition fullnameCol = new CmsListColumnDefinition(LIST_COLUMN_FULLNAME);
        fullnameCol.setName(Messages.get().container(Messages.GUI_USERS_LIST_COLS_FULLNAME_0));
        fullnameCol.setWidth("65%");
        fullnameCol.setTextWrapping(true);
        // add it to the list definition
        metadata.addColumn(fullnameCol);
    }

    /**
     * Sets the optional login default action.<p>
     * 
     * @param loginCol the login column
     */
    protected abstract void setDefaultAction(CmsListColumnDefinition loginCol);

    /**
     * Sets the needed icon action(s).<p>
     * 
     * @param iconCol the list column for edition.
     */
    protected abstract void setIconAction(CmsListColumnDefinition iconCol);

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setIndependentActions(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setIndependentActions(CmsListMetadata metadata) {

        // add other ou button
        CmsListItemDetails otherOuDetails = new CmsListItemDetails(LIST_DETAIL_OTHEROU);
        otherOuDetails.setVisible(false);
        otherOuDetails.setHideAction(new CmsListIndependentAction(LIST_DETAIL_OTHEROU) {

            /**
             * @see org.opencms.workplace.tools.A_CmsHtmlIconButton#getIconPath()
             */
            public String getIconPath() {

                return A_CmsListDialog.ICON_DETAILS_HIDE;
            }

            /**
             * @see org.opencms.workplace.tools.A_CmsHtmlIconButton#isVisible()
             */
            public boolean isVisible() {

                return ((A_CmsGroupUsersList)getWp()).hasUsersInOtherOus();
            }
        });
        otherOuDetails.setShowAction(new CmsListIndependentAction(LIST_DETAIL_OTHEROU) {

            /**
             * @see org.opencms.workplace.tools.A_CmsHtmlIconButton#getIconPath()
             */
            public String getIconPath() {

                return A_CmsListDialog.ICON_DETAILS_SHOW;
            }

            /**
             * @see org.opencms.workplace.tools.A_CmsHtmlIconButton#isVisible()
             */
            public boolean isVisible() {

                return ((A_CmsGroupUsersList)getWp()).hasUsersInOtherOus();
            }
        });
        otherOuDetails.setShowActionName(Messages.get().container(Messages.GUI_USERS_DETAIL_SHOW_OTHEROU_NAME_0));
        otherOuDetails.setShowActionHelpText(Messages.get().container(Messages.GUI_USERS_DETAIL_SHOW_OTHEROU_HELP_0));
        otherOuDetails.setHideActionName(Messages.get().container(Messages.GUI_USERS_DETAIL_HIDE_OTHEROU_NAME_0));
        otherOuDetails.setHideActionHelpText(Messages.get().container(Messages.GUI_USERS_DETAIL_HIDE_OTHEROU_HELP_0));
        otherOuDetails.setName(Messages.get().container(Messages.GUI_USERS_DETAIL_OTHEROU_NAME_0));
        otherOuDetails.setFormatter(new CmsListItemDetailsFormatter(Messages.get().container(
            Messages.GUI_USERS_DETAIL_OTHEROU_NAME_0)));
        metadata.addItemDetails(otherOuDetails);
    }

    /**
     * Sets the optional state change action column.<p>
     * 
     * @param metadata the list metadata object
     */
    protected abstract void setStateActionCol(CmsListMetadata metadata);

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#validateParamaters()
     */
    protected void validateParamaters() throws Exception {

        // test the needed parameters
        m_paramGroupname = getCms().readGroup(new CmsUUID(getParamGroupid())).getName();
    }
}