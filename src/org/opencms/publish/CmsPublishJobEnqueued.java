/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/publish/CmsPublishJobEnqueued.java,v $
 * Date   : $Date: 2007/07/04 16:57:15 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2006 Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.publish;

import org.opencms.db.CmsPublishList;
import org.opencms.report.I_CmsReport;

/**
 * Defines a read-only publish job that is still waiting for publishing.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.2 $
 * 
 * @since 6.5.5
 */
public class CmsPublishJobEnqueued extends CmsPublishJobBase {

    /**
     * Default constructor.<p>
     * 
     * @param publishJob the delegate publish job
     */
    protected CmsPublishJobEnqueued(CmsPublishJobInfoBean publishJob) {

        super(publishJob);
    }

    /**
     * Returns the time this object has been created.<p>
     *
     * @return the time this object has been created
     */
    public long getEnqueueTime() {

        return m_publishJob.getEnqueueTime();
    }

    /**
     * Returns the list of resources to publish.<p>
     *
     * @return the list of resources to publish
     */
    public CmsPublishList getPublishList() {

        return m_publishJob.getPublishList();
    }

    /**
     * Returns the report for this publish job.<p>
     * 
     * @return the report for this publish job
     */
    public I_CmsReport getReport() {

        return m_publishJob.getReport();
    }
}
