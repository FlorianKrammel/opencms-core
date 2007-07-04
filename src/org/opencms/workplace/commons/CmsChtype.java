/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/CmsChtype.java,v $
 * Date   : $Date: 2007/07/04 16:57:19 $
 * Version: $Revision: 1.22 $
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

package org.opencms.workplace.commons;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.loader.CmsLoaderException;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.explorer.CmsExplorerTypeSettings;
import org.opencms.workplace.list.A_CmsListResourceTypeDialog;
import org.opencms.workplace.list.CmsListItem;
import org.opencms.workplace.list.CmsListOrderEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * The change resource type dialog handles the change of a resource type of a single VFS file.<p>
 * 
 * The following files use this class:
 * <ul>
 * <li>/commons/chtype.jsp
 * </ul>
 * <p>
 * 
 * @author Andreas Zahner 
 * @author Peter Bonrad
 * 
 * @version $Revision: 1.22 $ 
 * 
 * @since 6.0.0 
 */
public class CmsChtype extends A_CmsListResourceTypeDialog {

    /** The dialog type.<p> */
    public static final String DIALOG_TYPE = "chtype";

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     */
    public CmsChtype(CmsJspActionElement jsp) {

        super(
            jsp,
            A_CmsListResourceTypeDialog.LIST_ID,
            Messages.get().container(Messages.GUI_CHTYPE_PLEASE_SELECT_0),
            A_CmsListResourceTypeDialog.LIST_COLUMN_NAME,
            CmsListOrderEnum.ORDER_ASCENDING,
            null);
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     */
    public CmsChtype(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        this(new CmsJspActionElement(context, req, res));
    }

    /**
     * Uploads the specified file and replaces the VFS file.<p>
     * 
     * @throws JspException if inclusion of error dialog fails
     */
    public void actionChtype() throws JspException {

        try {
            int newType = CmsResourceTypePlain.getStaticTypeId();
            try {
                // get new resource type id from request
                newType = Integer.parseInt(getParamSelectedType());
            } catch (NumberFormatException nf) {
                throw new CmsException(Messages.get().container(Messages.ERR_GET_RESTYPE_1, getParamSelectedType()));
            }
            // check the resource lock state
            checkLock(getParamResource());
            // change the resource type
            getCms().chtype(getParamResource(), newType);
            // close the dialog window
            actionCloseDialog();
        } catch (Throwable e) {
            // error changing resource type, show error dialog
            includeErrorpage(this, e);
        }
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#actionDialog()
     */
    public void actionDialog() throws JspException, ServletException, IOException {

        if (getAction() == ACTION_OK) {
            actionChtype();
            return;
        }

        super.actionDialog();
    }

    /**
     * Builds a default button row with a continue and cancel button.<p>
     * 
     * Override this to have special buttons for your dialog.<p>
     * 
     * @return the button row 
     */
    public String dialogButtons() {

        return dialogButtonsOkCancel(" onclick=\"submitChtype(form);\"", null);
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListResourceTypeDialog#getParamSelectedType()
     */
    public String getParamSelectedType() {

        String item = super.getParamSelectedType();
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(item)) {

            // determine resource type id of resource to change
            try {
                CmsResource res = getCms().readResource(getParamResource(), CmsResourceFilter.ALL);
                return Integer.toString(res.getTypeId());
            } catch (CmsException e) {
                // do nothing
            }
        }

        return item;
    }

    /**
     * Returns the html code to add directly before the list inside the form element.<p>
     * 
     * @return the html code to add directly before the list inside the form element
     */
    protected String customHtmlBeforeList() {

        StringBuffer result = new StringBuffer(256);

        result.append(dialogBlockStart(null));
        result.append(key(Messages.GUI_LABEL_TITLE_0));
        result.append(": ");
        result.append(getJsp().property("Title", getParamResource(), ""));
        result.append("<br>");
        result.append(key(Messages.GUI_LABEL_STATE_0));
        result.append(": ");
        try {
            result.append(getState());
        } catch (CmsException e) {
            // not so important ... just go on
        }
        result.append("<br>");
        result.append(key(Messages.GUI_LABEL_PERMALINK_0));
        result.append(": ");
        result.append(OpenCms.getLinkManager().getPermalink(getCms(), getParamResource()));
        result.append(dialogBlockEnd());
        result.append(dialogSpacer());

        return result.toString();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#customHtmlStart()
     */
    protected String customHtmlStart() {

        StringBuffer result = new StringBuffer(256);
        result.append(super.customHtmlStart());

        result.append("<script type='text/javascript'>\n");

        result.append("function submitChtype(theForm) {\n");
        result.append("\ttheForm.action.value = \"" + DIALOG_OK + "\";\n");
        result.append("\ttheForm.submit();\n");
        result.append("}\n\n");

        result.append("</script>");

        return result.toString();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#getListItems()
     */
    protected List getListItems() throws CmsException {

        List ret = new ArrayList();

        // get current Cms object
        CmsObject cms = getCms();

        // determine resource type id of resource to change
        CmsResource res = cms.readResource(getParamResource(), CmsResourceFilter.ALL);

        // get all available explorer type settings
        List resTypes = OpenCms.getWorkplaceManager().getExplorerTypeSettings();
        boolean isFolder = res.isFolder();

        // loop through all visible resource types
        for (int i = 0; i < resTypes.size(); i++) {
            boolean changeable = false;

            // get explorer type settings for current resource type
            CmsExplorerTypeSettings settings = (CmsExplorerTypeSettings)resTypes.get(i);

            // only if settings is a real resourcetype
            boolean isResourceType;
            try {
                OpenCms.getResourceManager().getResourceType(settings.getName());
                isResourceType = true;
            } catch (CmsLoaderException e) {
                isResourceType = false;
            }

            if (isResourceType) {
                int resTypeId = OpenCms.getResourceManager().getResourceType(settings.getName()).getTypeId();
                // determine if this resTypeId is changeable by currentResTypeId

                // changeable is true if current resource is a folder and this resource type also
                if (isFolder && OpenCms.getResourceManager().getResourceType(resTypeId).isFolder()) {
                    changeable = true;
                } else if (!isFolder && !OpenCms.getResourceManager().getResourceType(resTypeId).isFolder()) {

                    // changeable is true if current resource is NOT a folder and this resource type also NOT                    
                    changeable = true;
                }

                if (changeable) {

                    // determine if this resource type is editable for the current user
                    CmsPermissionSet permissions = settings.getAccess().getPermissions(cms, res);
                    if (!permissions.requiresWritePermission() || !permissions.requiresControlPermission()) {

                        // skip resource types without required write or create permissions
                        continue;
                    }

                    // add found setting to list
                    CmsListItem item = getList().newItem(Integer.toString(resTypeId));
                    item.set(LIST_COLUMN_NAME, key(settings.getKey()));
                    item.set(LIST_COLUMN_ICON, "<img src=\""
                        + getSkinUri()
                        + "filetypes/"
                        + settings.getIcon()
                        + "\" style=\"width: 16px; height: 16px;\" />");
                    ret.add(item);
                }
            }
        }

        return ret;
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {

        // first call of dialog
        setAction(ACTION_DEFAULT);

        super.initWorkplaceRequestValues(settings, request);

        // check the required permissions to change the resource type      
        if (!checkResourcePermissions(CmsPermissionSet.ACCESS_WRITE, false)) {
            // no write permissions for the resource, set cancel action to close dialog
            setParamAction(DIALOG_CANCEL);
        }

        // set the dialog type
        setParamDialogtype(DIALOG_TYPE);
        // set the action for the JSP switch 
        if (DIALOG_OK.equals(getParamAction())) {
            // ok button pressed, change file type
            setAction(ACTION_OK);
        } else if (DIALOG_LOCKS_CONFIRMED.equals(getParamAction())) {
            setAction(ACTION_LOCKS_CONFIRMED);
        } else if (DIALOG_CANCEL.equals(getParamAction())) {
            // cancel button pressed
            setAction(ACTION_CANCEL);
        } else {

            // build title for change file type dialog     
            setParamTitle(key(Messages.GUI_CHTYPE_1, new Object[] {CmsResource.getName(getParamResource())}));
        }
    }

}
