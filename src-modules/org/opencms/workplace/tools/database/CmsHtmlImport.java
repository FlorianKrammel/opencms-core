/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/tools/database/CmsHtmlImport.java,v $
 * Date   : $Date: 2007/07/04 16:57:10 $
 * Version: $Revision: 1.16 $
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

package org.opencms.workplace.tools.database;

import org.opencms.db.CmsDbIoException;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.file.types.CmsResourceTypePointer;
import org.opencms.file.types.CmsResourceTypeXmlPage;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.importexport.CmsImportExportException;
import org.opencms.loader.CmsResourceManager;
import org.opencms.lock.CmsLock;
import org.opencms.lock.CmsLockType;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsLink;
import org.opencms.report.I_CmsReport;
import org.opencms.staticexport.CmsLinkTable;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.page.CmsXmlPage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;

/**
 * This class implements the HTML->OpenCms Template converter for OpenCms 6.x.<p>
 * 
 * @author Michael Emmerich 
 * @author Armen Markarian 
 * @author Peter Bonrad
 * 
 * @version $Revision: 1.16 $ 
 * 
 * @since 6.0.0 
 */
public class CmsHtmlImport {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsHtmlImport.class);
    
    /** filename of the meta.properties file. */
    public static final String META_PROPERTIES = "meta.properties";

    /** the CmsObject to use. */
    private CmsObject m_cmsObject;

    /** the destination directory in the OpenCms VFS. */
    private String m_destinationDir;

    /** the download gallery name. */
    private String m_downloadGallery;

    /** the element name of the template. */
    private String m_element;

    /** the end pattern for extracting content. */
    private String m_endPattern;

    /** HashMap of all known extensions in OpenCms. */
    private Map m_extensions;

    /** Storage for external links. */
    private HashSet m_externalLinks;

    /** The file index contains all resourcenames in the real file system and their renamed ones in the OpenCms VFS. */
    private HashMap m_fileIndex;

    /** the HTML converter to parse and modifiy the content. */
    private CmsHtmlImportConverter m_htmlConverter;

    /** the image gallery name. */
    private String m_imageGallery;

    /** Storage for image alt tags, it is filled by the HtmlConverter each time a new image is found. */
    private HashMap m_imageInfo;

    /** the input directory in the "real" file system. */
    private String m_inputDir;

    /** the encoding used for all imported input files. */
    private String m_inputEncoding;

    /** should broken links be kept. */
    private boolean m_keepBrokenLinks;

    /** leave downloads at the original location. */
    private boolean m_leaveDownloads;

    /** leave external links at the original location. */
    private boolean m_leaveExternalLinks;

    /** leave images at the original location. */
    private boolean m_leaveImages;

    /** the external link gallery name. */
    private String m_linkGallery;

    /** the locale use for content definition. */
    private String m_locale;

    /** the overwrite value new resources. */
    private boolean m_overwrite;

    /** Map with all parents in file system to OpenCms. */
    private HashMap m_parents;

    /** the report for the output. */
    private I_CmsReport m_report;

    /** the start pattern for extracting content. */
    private String m_startPattern;

    /** the template use for all pages. */
    private String m_template;

    /**
     * Default Constructor.<p>
     */
    public CmsHtmlImport() {

        m_overwrite = true;
        m_extensions = OpenCms.getResourceManager().getExtensionMapping();
        m_fileIndex = new HashMap();
        m_parents = new HashMap();
        m_imageInfo = new HashMap();
        m_externalLinks = new HashSet();
        m_htmlConverter = new CmsHtmlImportConverter(this, false);
    }

    /**
     * Creates a new import object for the given cms object.<p>
     * 
     * @param cms the current cms context
     */
    public CmsHtmlImport(CmsObject cms) {

        this();
        m_cmsObject = cms;
    }

    /**
     * Calculates an absolute uri from a relative "uri" and the given absolute "baseUri".<p> 
     * 
     * If "uri" is already absolute, it is returned unchanged.
     * This method also returns "uri" unchanged if it is not well-formed.<p>
     *    
     * @param relativeUri the relative uri to calculate an absolute uri for
     * @param baseUri the base uri, this must be an absolute uri
     * @return an absolute uri calculated from "uri" and "baseUri"
     */
    public String getAbsoluteUri(String relativeUri, String baseUri) {

        if ((relativeUri == null) || (relativeUri.charAt(0) == '/') || (relativeUri.startsWith("#"))) {

            return relativeUri;
        }

        // if we are on a windows system, we must add a ":" in the uri later               
        String windowsAddition = "";
        if (File.separator.equals("\\")) {
            windowsAddition = ":";
        }

        try {
            URL baseUrl = new URL("file://");
            URL url = new URL(new URL(baseUrl, "file://" + baseUri), relativeUri);
            if (url.getQuery() == null) {
                if (url.getRef() == null) {
                    return url.getHost() + windowsAddition + url.getPath();
                } else {
                    return url.getHost() + windowsAddition + url.getPath() + "#" + url.getRef();
                }
            } else {
                return url.getHost() + windowsAddition + url.getPath() + "?" + url.getQuery();
            }
        } catch (MalformedURLException e) {
            return relativeUri;
        }
    }

    /**
     * Returns the destinationDir.<p>
     *
     * @return the destinationDir
     */
    public String getDestinationDir() {

        return m_destinationDir;
    }

    /**
     * Returns the downloadGallery.<p>
     *
     * @return the downloadGallery
     */
    public String getDownloadGallery() {

        return m_downloadGallery;
    }

    /**
     * Returns the element.<p>
     *
     * @return the element
     */
    public String getElement() {

        return m_element;
    }

    /**
     * Returns the endPattern.<p>
     *
     * @return the endPattern
     */
    public String getEndPattern() {

        return m_endPattern;
    }

    /**
     * Returns the imageGallery.<p>
     *
     * @return the imageGallery
     */
    public String getImageGallery() {

        return m_imageGallery;
    }

    /**
     * Returns the inputDir.<p>
     *
     * @return the inputDir
     */
    public String getInputDir() {

        return m_inputDir;
    }

    /**
     * Returns the inputEncoding.<p>
     *
     * @return the inputEncoding
     */
    public String getInputEncoding() {

        return m_inputEncoding;
    }

    /**
     * Returns the linkGallery.<p>
     *
     * @return the linkGallery
     */
    public String getLinkGallery() {

        return m_linkGallery;
    }

    /**
     * Returns the locale.<p>
     *
     * @return the locale
     */
    public String getLocale() {

        return m_locale;
    }

    /**
     * Returns the startPattern.<p>
     *
     * @return the startPattern
     */
    public String getStartPattern() {

        return m_startPattern;
    }

    /**
     * Returns the template.<p>
     *
     * @return the template
     */
    public String getTemplate() {

        return m_template;
    }

    /**
     * Returns the keepBrokenLinks.<p>
     *
     * @return the keepBrokenLinks
     */
    public boolean isKeepBrokenLinks() {

        return m_keepBrokenLinks;
    }

    /**
     * Returns the leaveDownloads.<p>
     *
     * @return the leaveDownloads
     */
    public boolean isLeaveDownloads() {

        return m_leaveDownloads;
    }

    /**
     * Returns the leaveExternalLinks.<p>
     *
     * @return the leaveExternalLinks
     */
    public boolean isLeaveExternalLinks() {

        return m_leaveExternalLinks;
    }

    /**
     * Returns the leaveImages.<p>
     *
     * @return the leaveImages
     */
    public boolean isLeaveImages() {

        return m_leaveImages;
    }

    /**
     * Returns the overwrite.<p>
     *
     * @return the overwrite
     */
    public boolean isOverwrite() {

        return m_overwrite;
    }

    /**
     * Sets the destinationDir.<p>
     *
     * @param destinationDir the destinationDir to set
     */
    public void setDestinationDir(String destinationDir) {

        m_destinationDir = destinationDir;
    }

    /**
     * Sets the downloadGallery.<p>
     *
     * @param downloadGallery the downloadGallery to set
     */
    public void setDownloadGallery(String downloadGallery) {

        m_downloadGallery = downloadGallery;
    }

    /**
     * Sets the element.<p>
     *
     * @param element the element to set
     */
    public void setElement(String element) {

        m_element = element;
    }

    /**
     * Sets the endPattern.<p>
     *
     * @param endPattern the endPattern to set
     */
    public void setEndPattern(String endPattern) {

        m_endPattern = endPattern;
    }

    /**
     * Sets the imageGallery.<p>
     *
     * @param imageGallery the imageGallery to set
     */
    public void setImageGallery(String imageGallery) {

        m_imageGallery = imageGallery;
    }

    /**
     * Sets the inputDir.<p>
     *
     * @param inputDir the inputDir to set
     */
    public void setInputDir(String inputDir) {

        m_inputDir = inputDir;
    }

    /**
     * Sets the inputEncoding.<p>
     *
     * @param inputEncoding the inputEncoding to set
     */
    public void setInputEncoding(String inputEncoding) {

        m_inputEncoding = inputEncoding;
    }

    /**
     * Sets the keepBrokenLinks.<p>
     *
     * @param keepBrokenLinks the keepBrokenLinks to set
     */
    public void setKeepBrokenLinks(boolean keepBrokenLinks) {

        m_keepBrokenLinks = keepBrokenLinks;
    }

    /**
     * Sets the leaveDownloads.<p>
     *
     * @param leaveDownloads the leaveDownloads to set
     */
    public void setLeaveDownloads(boolean leaveDownloads) {

        m_leaveDownloads = leaveDownloads;
    }

    /**
     * Sets the leaveExternalLinks.<p>
     *
     * @param leaveExternalLinks the leaveExternalLinks to set
     */
    public void setLeaveExternalLinks(boolean leaveExternalLinks) {

        m_leaveExternalLinks = leaveExternalLinks;
    }

    /**
     * Sets the leaveImages.<p>
     *
     * @param leaveImages the leaveImages to set
     */
    public void setLeaveImages(boolean leaveImages) {

        m_leaveImages = leaveImages;
    }

    /**
     * Sets the linkGallery.<p>
     *
     * @param linkGallery the linkGallery to set
     */
    public void setLinkGallery(String linkGallery) {

        m_linkGallery = linkGallery;
    }

    /**
     * Sets the locale.<p>
     *
     * @param locale the locale to set
     */
    public void setLocale(String locale) {

        m_locale = locale;
    }

    /**
     * Sets the overwrite.<p>
     *
     * @param overwrite the overwrite to set
     */
    public void setOverwrite(boolean overwrite) {

        m_overwrite = overwrite;
    }

    /**
     * Sets the startPattern.<p>
     *
     * @param startPattern the startPattern to set
     */
    public void setStartPattern(String startPattern) {

        m_startPattern = startPattern;
    }

    /**
     * Sets the template.<p>
     *
     * @param template the template to set
     */
    public void setTemplate(String template) {

        m_template = template;
    }

    /**
     * Imports all resources from the real filesystem, stores them into the correct locations
     * in the OpenCms VFS and modifies all links. This method is called form the JSP to start the
     * import process.<p>
     * @param report StringBuffer for reporting
     * @throws Exception if something goes wrong
     */
    public void startImport(I_CmsReport report) throws Exception {

        try {
            m_report = report;
            m_report.println(Messages.get().container(Messages.RPT_HTML_IMPORT_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);

            // first build the index of all resources
            buildIndex(m_inputDir);
            // build list with all parent resources of input directory for links to outside import folder
            buildParentPath();
            // copy and parse all html files first. during the copy process we will collect all 
            // required data for downloads and images
            copyHtmlFiles(m_inputDir);
            // now copy the other files
            copyOtherFiles(m_inputDir);
            // finally create all the external links    
            createExternalLinks();
            m_report.println(Messages.get().container(Messages.RPT_HTML_IMPORT_END_0), I_CmsReport.FORMAT_HEADLINE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new external link to the storage of external links.<p>
     * 
     * All links in this storage are later used to create entries in the external link gallery.
     * @param externalLink link to an external resource
     * @return the complete path to the external link file, if one is created.
     */
    public String storeExternalLink(String externalLink) {

        if (!m_leaveExternalLinks) {
            m_externalLinks.add(externalLink);
            return getExternalLinkFile(externalLink);
        }

        return null;
    }

    /**
     * Add a new image info to the storage of image infos.<p>
     * 
     * The image infoes are later used to set the description properties of the images.
     * @param image the name of the image
     * @param altText the alt-text of the image
     */
    public void storeImageInfo(String image, String altText) {

        m_imageInfo.put(image, altText);
    }

    /**
     * Translated a link into the real filesystem to its new location in the OpenCms VFS.<p>
     * 
     * This is needed by the HtmlConverter to get the correct links for link translation.
     * @param link link to the real filesystem
     * @return string containing absulute link into the OpenCms VFS
     */
    public String translateLink(String link) {

        String translatedLink = null;
        translatedLink = (String)m_fileIndex.get(link.replace('\\', '/'));

        if (translatedLink == null) {
            // its an anchor link, so copy use it
            if (link.startsWith("#")) {
                translatedLink = link;
            }

            // relative link to opencms root
            else if (link.startsWith("/")) {

                // strip cms context path
                if (link.startsWith(OpenCms.getSystemInfo().getOpenCmsContext())) {
                    link = link.substring(OpenCms.getSystemInfo().getOpenCmsContext().length());
                }

                // check if resource exists
                if ((m_keepBrokenLinks) || (m_cmsObject.existsResource(link))) {
                    translatedLink = link;
                }
            }

            else {

                String fileBase = getBasePath(m_inputDir, link);
                String cmsBase = (String)m_parents.get(fileBase);
                if (cmsBase != null) {
                    String outLink = cmsBase + link.substring(fileBase.length()).replace('\\', '/');
                    if ((m_keepBrokenLinks) || (m_cmsObject.existsResource(outLink))) {
                        translatedLink = outLink;
                    }
                }
            }
        }

        // if the link goes to a directory, lets link to the index page within
        if ((translatedLink != null) && translatedLink.endsWith("/")) {
            translatedLink += "index.html";
        }

        // final check: if the translated link is still null the original link found
        // was broken
        // lets link it to the same page, the link is found on
        if (translatedLink == null) {
            translatedLink = "#";
        }

        return translatedLink;
    }

    /**
     * Tests if all given input parameters for the HTML Import are valid, i.e. that all the 
     * given folders do exist. <p>
     * 
     * @throws CmsIllegalArgumentException if some parameters are not valid
     */
    public void validate() throws CmsIllegalArgumentException {

        // check the input directory
        File inputDir = new File(m_inputDir);
        if (!inputDir.exists() || inputDir.isFile()) {
            // the input directory is not valid
            throw new CmsIllegalArgumentException(Messages.get().container(
                Messages.GUI_HTMLIMPORT_INPUTDIR_1,
                m_inputDir));
        }

        // check the destination directory        
        try {
            if (CmsStringUtil.isEmpty(m_destinationDir)) {
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.GUI_HTMLIMPORT_DESTDIR_1,
                    m_destinationDir));
            }
            m_cmsObject.readFolder(m_destinationDir);
        } catch (CmsException e) {
            // an excpetion is thrown if the folder does not exist
            throw new CmsIllegalArgumentException(Messages.get().container(
                Messages.GUI_HTMLIMPORT_DESTDIR_1,
                m_destinationDir), e);
        }

        // check the image gallery
        // only if flag for leaving images at original location is off
        if (!m_leaveImages) {
            try {
                CmsFolder folder = m_cmsObject.readFolder(m_imageGallery);
                // check if folder is a image gallery
                String name = OpenCms.getResourceManager().getResourceType(folder.getTypeId()).getTypeName();
                if (!name.equals("imagegallery")) {
                    throw new CmsIllegalArgumentException(Messages.get().container(
                        Messages.GUI_HTMLIMPORT_IMGGALLERY_INVALID_1,
                        m_imageGallery));
                }
            } catch (CmsException e) {
                // an excpetion is thrown if the folder does not exist
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.GUI_HTMLIMPORT_IMGGALLERY_1,
                    m_imageGallery), e);
            }
        }

        // check the link gallery
        // only if flag for leaving external links at original location is off
        if (!m_leaveExternalLinks) {
            try {
                CmsFolder folder = m_cmsObject.readFolder(m_linkGallery);
                // check if folder is a link gallery
                String name = OpenCms.getResourceManager().getResourceType(folder.getTypeId()).getTypeName();
                if (!name.equals("linkgallery")) {
                    throw new CmsIllegalArgumentException(Messages.get().container(
                        Messages.GUI_HTMLIMPORT_LINKGALLERY_INVALID_1,
                        m_linkGallery));
                }
            } catch (CmsException e) {
                // an excpetion is thrown if the folder does not exist
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.GUI_HTMLIMPORT_LINKGALLERY_1,
                    m_linkGallery), e);
            }
        }

        // check the download gallery
        if ((!isExternal(m_downloadGallery)) && (!m_leaveDownloads)) {
            try {
                CmsFolder folder = m_cmsObject.readFolder(m_downloadGallery);
                // check if folder is a download gallery
                String name = OpenCms.getResourceManager().getResourceType(folder.getTypeId()).getTypeName();
                if (!name.equals("downloadgallery")) {
                    throw new CmsIllegalArgumentException(Messages.get().container(
                        Messages.GUI_HTMLIMPORT_DOWNGALLERY_INVALID_1,
                        m_downloadGallery));
                }
            } catch (CmsException e) {
                // an excpetion is thrown if the folder does not exist
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.GUI_HTMLIMPORT_DOWNGALLERY_1,
                    m_downloadGallery), e);
            }
        }

        // check the template
        try {
            m_cmsObject.readResource(m_template, CmsResourceFilter.ALL);
        } catch (CmsException e) {
            // an excpetion is thrown if the template does not exist
            if (!isValidElement()) {
                throw new CmsIllegalArgumentException(Messages.get().container(
                    Messages.GUI_HTMLIMPORT_TEMPLATE_1,
                    m_template), e);
            }
        }

        // check the element
        if (!isValidElement()) {
            throw new CmsIllegalArgumentException(Messages.get().container(
                Messages.GUI_HTMLIMPORT_INVALID_ELEM_2,
                m_element,
                m_template));
        }

        // check if we are in an offline project
        if (m_cmsObject.getRequestContext().currentProject().isOnlineProject()) {
            throw new CmsIllegalArgumentException(
                Messages.get().container(Messages.GUI_HTMLIMPORT_CONSTRAINT_OFFLINE_0));
        }
    }

    /**
     * Builds an index of all files to be imported and determines their new names in the OpenCms.<p>
     * 
     * @param startfolder the folder to start with
     * 
     * @throws Exception if something goes wrong
     */
    private void buildIndex(String startfolder) throws Exception {

        File folder = new File(startfolder);
        // get all subresources

        File[] subresources = folder.listFiles();
        // now loop through all subresources and add them to the index list
        for (int i = 0; i < subresources.length; i++) {
            try {

                String relativeFSName = subresources[i].getAbsolutePath().substring(m_inputDir.length() + 1);
                String absoluteVFSName = getVfsName(relativeFSName, subresources[i].getName(), subresources[i].isFile());
                m_report.print(Messages.get().container(Messages.RPT_CREATE_INDEX_0), I_CmsReport.FORMAT_NOTE);
                m_report.print(org.opencms.report.Messages.get().container(
                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                    relativeFSName.replace('\\', '/')));
                m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
                m_report.print(Messages.get().container(Messages.RPT_ARROW_RIGHT_0), I_CmsReport.FORMAT_NOTE);
                m_report.print(org.opencms.report.Messages.get().container(
                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                    absoluteVFSName));
                m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
                m_fileIndex.put(subresources[i].getAbsolutePath().replace('\\', '/'), absoluteVFSName);
                // if the subresource is a folder, get all subresources of it as well
                if (subresources[i].isDirectory()) {
                    buildIndex(subresources[i].getAbsolutePath());
                }
                m_report.println(
                    org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } catch (Exception e) {
                LOG.error(e.getLocalizedMessage(), e);
                m_report.println(e);
            }
        }
    }

    /**
     * Builds a map with all parents of the destination dir to the real file system.
     * So links to resources of outside the import folder can be found.
     *
     */
    private void buildParentPath() {

        String destFolder = m_destinationDir;
        String inputDir = m_inputDir.replace('\\', '/');
        if (!inputDir.endsWith("/")) {
            inputDir += "/";
        }
        int pos = inputDir.lastIndexOf("/");
        while ((pos > 0) && (destFolder != null)) {
            inputDir = inputDir.substring(0, pos);
            m_parents.put(inputDir + "/", destFolder);
            
            pos = inputDir.lastIndexOf("/", pos-1);
            destFolder = CmsResource.getParentFolder(destFolder);
        }
    }

    /**
     * Copies all  HTML files to the VFS.<p>
     * 
     * @param startfolder startfolder the folder to start with
     * 
     * @throws Exception if something goes wrong
     */
    private void copyHtmlFiles(String startfolder) throws Exception {

        try {
            File folder = new File(startfolder);
            // get all subresources
            File[] subresources = folder.listFiles();
            // now loop through all subresources 
            for (int i = 0; i < subresources.length; i++) {
                // if the subresource is a folder, get all subresources of it as well          
                if (subresources[i].isDirectory()) {
                    // first, create the folder in the VFS    
                    Hashtable properties = new Hashtable();
                    createFolder(subresources[i].getAbsolutePath(), i, properties);
                    // now process all rescources inside of the folder
                    copyHtmlFiles(subresources[i].getAbsolutePath());
                } else {
                    // create a new file in the VFS      
                    String vfsFileName = (String)m_fileIndex.get(subresources[i].getAbsolutePath().replace('\\', '/'));
                    // check if this is an Html file, do only import and parse those
                    int type = getFileType(vfsFileName);
                    if (CmsResourceTypePlain.getStaticTypeId() == type) {
                        Hashtable properties = new Hashtable();
                        // the subresource is a file, so start the parsing process
                        String content = "";
                        try {
                            content = parseHtmlFile(subresources[i], properties);
                        } catch (CmsException e) {
                            m_report.println(e);
                        }
                        properties.put("template", m_template);

                        // create the file in the VFS
                        createFile(subresources[i].getAbsolutePath(), i, content, properties);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Copies all files except HTML files to the VFS.<p>
     * 
     * @param startfolder startfolder the folder to start with
     */
    private void copyOtherFiles(String startfolder) {

        try {
            File folder = new File(startfolder);
            // get all subresources
            File[] subresources = folder.listFiles();
            // now loop through all subresources 
            for (int i = 0; i < subresources.length; i++) {
                // if the subresource is a folder, get all subresources of it as well
                if (subresources[i].isDirectory()) {
                    copyOtherFiles(subresources[i].getAbsolutePath());
                } else {
                    // do not import the "meta.properties" file
                    if (!subresources[i].getName().equals(META_PROPERTIES)) {
                        // create a new file in the VFS      
                        String vfsFileName = (String)m_fileIndex.get(subresources[i].getAbsolutePath().replace(
                            '\\',
                            '/'));
                        // get the file type of the FS file
                        int type = getFileType(vfsFileName);
                        if (CmsResourceTypePlain.getStaticTypeId() != type) {

                            if (isExternal(vfsFileName)) {

                                m_report.print(
                                    Messages.get().container(Messages.RPT_SKIP_EXTERNAL_0),
                                    I_CmsReport.FORMAT_NOTE);
                                m_report.print(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                                    subresources[i]));
                                m_report.print(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_DOTS_0));
                                m_report.print(
                                    Messages.get().container(Messages.RPT_ARROW_RIGHT_0),
                                    I_CmsReport.FORMAT_NOTE);
                                m_report.println(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                                    vfsFileName));
                            } else {

                                m_report.print(Messages.get().container(Messages.RPT_IMPORT_0), I_CmsReport.FORMAT_NOTE);
                                m_report.print(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                                    vfsFileName));
                                m_report.print(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_DOTS_0));

                                // get the content of the FS file
                                byte[] content = getFileBytes(subresources[i]);
                                // get the filename from the fileIndex list

                                // check if there are some image infos stored for this resource
                                List properties = new ArrayList();
                                String altText = (String)m_imageInfo.get(subresources[i].getAbsolutePath().replace(
                                    '\\',
                                    '/'));
                                CmsProperty property1 = new CmsProperty(
                                    CmsPropertyDefinition.PROPERTY_DESCRIPTION,
                                    altText,
                                    altText);
                                CmsProperty property2 = new CmsProperty(
                                    CmsPropertyDefinition.PROPERTY_TITLE,
                                    altText,
                                    altText);
                                // add them to the title and description property
                                if (altText != null) {
                                    properties.add(property1);
                                    properties.add(property2);
                                }
                                // create the file
                                if (!m_overwrite) {
                                    m_cmsObject.createResource(vfsFileName, type, content, properties);
                                } else {
                                    try {
                                        CmsLock lock = m_cmsObject.getLock(vfsFileName);
                                        if (lock.getType() != CmsLockType.EXCLUSIVE) {
                                            m_cmsObject.lockResource(vfsFileName);
                                        }
                                        m_cmsObject.deleteResource(vfsFileName, CmsResource.DELETE_PRESERVE_SIBLINGS);
                                    } catch (CmsException e) {
                                        // the file did not exist, so create it                                     
                                    } finally {
                                        m_cmsObject.createResource(vfsFileName, type, content, properties);
                                    }

                                    m_report.print(
                                        Messages.get().container(Messages.RPT_OVERWRITE_0),
                                        I_CmsReport.FORMAT_NOTE);
                                    m_report.print(org.opencms.report.Messages.get().container(
                                        org.opencms.report.Messages.RPT_DOTS_0));
                                }
                                m_report.println(org.opencms.report.Messages.get().container(
                                    org.opencms.report.Messages.RPT_OK_0), I_CmsReport.FORMAT_OK);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            m_report.println(e);
        }
    }

    /**
     * Creates all external links, which were found during the HTML-page processing.<p>
     * 
     */
    private void createExternalLinks() {

        // loop through all links
        Iterator i = m_externalLinks.iterator();
        while (i.hasNext()) {
            String linkUrl = (String)i.next();
            String filename = getExternalLinkFile(linkUrl);

            m_report.print(Messages.get().container(Messages.RPT_CREATE_EXTERNAL_LINK_0), I_CmsReport.FORMAT_NOTE);
            m_report.print(org.opencms.report.Messages.get().container(
                org.opencms.report.Messages.RPT_ARGUMENT_1,
                filename));
            m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));

            List properties = new ArrayList();
            CmsProperty property1 = new CmsProperty(
                CmsPropertyDefinition.PROPERTY_TITLE,
                "Link to " + linkUrl,
                "Link to " + linkUrl);
            properties.add(property1);
            try {
                m_cmsObject.createResource(
                    m_linkGallery + filename,
                    CmsResourceTypePointer.getStaticTypeId(),
                    linkUrl.getBytes(),
                    properties);
            } catch (CmsException e) {
                // do nothing here, an exception will be thrown if this link already exisits                
            }
            m_report.println(
                org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0),
                I_CmsReport.FORMAT_OK);
        }
    }

    /**
     * Creates a file in the VFS.<p>
     * 
     * @param filename the complete filename in the real file system
     * @param position the default nav pos of this folder
     * @param content the html content of the file
     * @param properties the file properties
     */
    private void createFile(String filename, int position, String content, Hashtable properties) {

        String vfsFileName = (String)m_fileIndex.get(filename.replace('\\', '/'));

        if (vfsFileName != null) {
            try {

                m_report.print(Messages.get().container(Messages.RPT_CREATE_FILE_0), I_CmsReport.FORMAT_NOTE);
                m_report.print(org.opencms.report.Messages.get().container(
                    org.opencms.report.Messages.RPT_ARGUMENT_1,
                    vfsFileName));
                m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));

                // check if we have to set the navpos property.
                if ((properties.get(CmsPropertyDefinition.PROPERTY_NAVPOS) == null)
                    && (properties.get(CmsPropertyDefinition.PROPERTY_NAVTEXT) != null)) {
                    // set the position in the folder as navpos
                    // we have to add one to the postion, since it is counted from 0
                    properties.put(CmsPropertyDefinition.PROPERTY_NAVPOS, (position + 1) + "");
                }

                // create new xml page
                Locale locale = new Locale(m_locale);
                CmsXmlPage page = new CmsXmlPage(locale, OpenCms.getSystemInfo().getDefaultEncoding());
                page.addValue(m_element, locale);
                page.setStringValue(m_cmsObject, m_element, locale, content);

                // check links
                CmsLinkTable linkTable = page.getLinkTable(m_element, locale);
                Iterator i = linkTable.iterator();
                while (i.hasNext()) {
                    CmsLink link = (CmsLink)i.next();
                    String target = link.getTarget();
                    // do only update internal links 
                    if (link.isInternal()) {
                        target = m_cmsObject.getRequestContext().getFileTranslator().translateResource(target);
                        // update link
                        link.updateLink(target, link.getAnchor(), link.getQuery());
                        link.checkConsistency(m_cmsObject);
                    }
                }
                // marshal xml page and get the content
                byte[] contentByteArray = page.marshal();
                List oldProperties = new ArrayList();

                if (!m_overwrite) {
                    m_cmsObject.createResource(
                        vfsFileName,
                        CmsResourceTypeXmlPage.getStaticTypeId(),
                        contentByteArray,
                        new ArrayList());
                } else {
                    try {
                        // try if the file is there
                        oldProperties = m_cmsObject.readPropertyObjects(vfsFileName, false);
                        CmsLock lock = m_cmsObject.getLock(vfsFileName);
                        if (lock.getType() != CmsLockType.EXCLUSIVE) {
                            m_cmsObject.lockResource(vfsFileName);
                        }
                        m_cmsObject.deleteResource(vfsFileName, CmsResource.DELETE_PRESERVE_SIBLINGS);
                    } catch (CmsException e) {
                        // the file did not exist, so we do not have to delete it                      
                    } finally {
                        // create the new resource
                        m_report.print(Messages.get().container(Messages.RPT_OVERWRITE_0), I_CmsReport.FORMAT_NOTE);
                        m_report.print(org.opencms.report.Messages.get().container(
                            org.opencms.report.Messages.RPT_DOTS_0));
                        m_cmsObject.createResource(
                            vfsFileName,
                            CmsResourceTypeXmlPage.getStaticTypeId(),
                            contentByteArray,
                            new ArrayList());
                    }
                }
                // create all properties and put them in an ArrayList
                Iterator it = properties.entrySet().iterator();
                List propertyList = new ArrayList();
                while (it.hasNext()) {
                    // get property and value
                    Map.Entry entry = (Map.Entry)it.next();
                    String propertyKey = (String)entry.getKey();
                    String propertyVal = (String)entry.getValue();
                    // create new Property Object
                    CmsProperty property = new CmsProperty(propertyKey, propertyVal, propertyVal);
                    // create implicitly if Property doesn't exist already
                    property.setAutoCreatePropertyDefinition(true);
                    // add new property to the list
                    propertyList.add(property);
                }
                // try to write the properties
                try {
                    m_cmsObject.writePropertyObjects(vfsFileName, propertyList);
                    // write the old properties if available
                    m_cmsObject.writePropertyObjects(vfsFileName, oldProperties);
                } catch (CmsException e1) {
                    e1.printStackTrace();
                }
                m_report.println(
                    org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } catch (CmsException e) {
                m_report.println(e);
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Creates a folder in the VFS.<p>
     * 
     * @param foldername the complete foldername in the real file system
     * @param position the default nav pos of this folder
     * @param properties the file properties
     */
    private void createFolder(String foldername, int position, Hashtable properties) {

        String vfsFolderName = (String)m_fileIndex.get(foldername.replace('\\', '/'));

        m_report.print(Messages.get().container(Messages.RPT_CREATE_FOLDER_0), I_CmsReport.FORMAT_NOTE);
        m_report.print(org.opencms.report.Messages.get().container(
            org.opencms.report.Messages.RPT_ARGUMENT_1,
            vfsFolderName));
        m_report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));

        if (vfsFolderName != null) {
            String path = vfsFolderName.substring(
                0,
                vfsFolderName.substring(0, vfsFolderName.length() - 1).lastIndexOf("/"));
            String folder = vfsFolderName.substring(path.length(), vfsFolderName.length());
            try {
                // try to find a meta.properties file in the folder
                String propertyFileName = foldername + File.separator + META_PROPERTIES;

                boolean metaPropertiesFound = false;
                ExtendedProperties propertyFile = new ExtendedProperties();
                try {
                    propertyFile.load(new FileInputStream(new File(propertyFileName)));
                    metaPropertiesFound = true;
                } catch (Exception e1) {
                    // do nothing if the propertyfile could not be loaded since it is not required
                    // that such s file does exist
                }
                // now copy all values from the propertyfile to the already found properties of the
                // new folder in OpenCms
                // only do this if we have found a meta.properties file          
                if (metaPropertiesFound) {
                    Enumeration enu = propertyFile.keys();
                    String property = "";
                    while (enu.hasMoreElements()) {
                        // get property and value
                        try {
                            property = (String)enu.nextElement();
                            String propertyvalue = (String)propertyFile.get(property);
                            // copy to the properties of the OpenCms folder
                            properties.put(property, propertyvalue);
                        } catch (Exception e2) {
                            // just skip this property if it could ne be read.
                            e2.printStackTrace();
                        }
                    }

                    // check if we have to set the navpos property.
                    if (properties.get(CmsPropertyDefinition.PROPERTY_NAVPOS) == null) {
                        // set the position in the folder as navpos
                        // we have to add one to the postion, since it is counted from 0
                        properties.put(CmsPropertyDefinition.PROPERTY_NAVPOS, (position + 1) + "");
                    }
                    // check if we have to set the navpos property.
                    if (properties.get(CmsPropertyDefinition.PROPERTY_NAVTEXT) == null) {
                        // set the foldername in the folder as navtext
                        String navtext = folder.substring(1, 2).toUpperCase()
                            + folder.substring(2, folder.length() - 1);
                        properties.put(CmsPropertyDefinition.PROPERTY_NAVTEXT, navtext);
                    }
                } else {
                    // if there was no meta.properties file, no properties should be added to the
                    // folder
                    properties = new Hashtable();
                }
                // try to read the folder, it its there we must not create it again
                try {
                    m_cmsObject.readFolder(path + folder);
                    m_cmsObject.lockResource(path + folder);
                } catch (CmsException e1) {
                    // the folder was not there, so create it
                    m_cmsObject.createResource(path + folder, CmsResourceTypeFolder.getStaticTypeId());
                }
                // create all properties and put them in an ArrayList
                Enumeration enu = properties.keys();
                List propertyList = new ArrayList();
                while (enu.hasMoreElements()) {
                    // get property and value
                    String propertyKey = (String)enu.nextElement();
                    String propertyVal = (String)properties.get(propertyKey);
                    CmsProperty property = new CmsProperty(propertyKey, propertyVal, propertyVal);
                    // create implicitly if Property doesn't exist already
                    property.setAutoCreatePropertyDefinition(true);
                    // add new property to the list
                    propertyList.add(property);
                }
                // try to write the property Objects
                try {
                    m_cmsObject.writePropertyObjects(path + folder, propertyList);
                } catch (CmsException e1) {
                    e1.printStackTrace();
                }
                m_report.println(
                    org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            } catch (CmsException e) {
                m_report.println(e);
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Compares 2 pathes for the base part which have both equal.
     * 
     * @param path1 the first path to compare
     * @param path2 the second path to compare
     * @return the base path of both which are equal
     */
    private String getBasePath(String path1, String path2) {

        StringBuffer base = new StringBuffer();
        path1 = path1.replace('\\', '/');
        path2 = path2.replace('\\', '/');

        String[] parts1 = path1.split("/");
        String[] parts2 = path2.split("/");

        for (int i = 0; i < parts1.length; i++) {
            if (i >= parts2.length) {
                break;
            }
            if (parts1[i].equals(parts2[i])) {
                base.append(parts1[i] + "/");
            }
        }

        return base.toString();
    }

    /**
     * Creates the filename of the file of the external link.
     * 
     * @param link the link to get the file path for.
     * @return the filename of the file for the external link.
     */
    private String getExternalLinkFile(String link) {

        String filename = link.substring(link.indexOf("://") + 3, link.length());
        filename = m_cmsObject.getRequestContext().getFileTranslator().translateResource(filename.replace('/', '-'));
        return filename;
    }

    /**
     * Returns a byte array containing the content of server FS file.<p>
     *
     * @param file the name of the file to read
     * @return bytes[] the content of the file
     * @throws CmsException if something goes wrong
     */
    private byte[] getFileBytes(File file) throws CmsException {

        byte[] buffer = null;

        FileInputStream fileStream = null;
        int charsRead;
        int size;
        try {
            fileStream = new FileInputStream(file);
            charsRead = 0;
            size = new Long(file.length()).intValue();
            buffer = new byte[size];
            while (charsRead < size) {
                charsRead += fileStream.read(buffer, charsRead, size - charsRead);
            }
            return buffer;
        } catch (IOException e) {
            throw new CmsDbIoException(
                Messages.get().container(Messages.ERR_GET_FILE_BYTES_1, file.getAbsolutePath()),
                e);
        } finally {
            try {
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the OpenCms file type of a real filesystem file. <p>
     * 
     * This is made by checking the extension.
     * 
     * @param filename the name of the file in the real filesystem  
     * 
     * @return the id of the OpenCms file type
     * 
     * @throws Exception if something goes wrong
     */
    private int getFileType(String filename) throws Exception {

        String extension = "";
        if (filename.indexOf(".") > -1) {
            extension = filename.substring((filename.lastIndexOf(".")));
        }

        String typename = (String)m_extensions.get(extension.toLowerCase());
        if (typename == null) {
            typename = "binary";
        }
        CmsResourceManager resourceManager = OpenCms.getResourceManager();

        return resourceManager.getResourceType(typename).getTypeId();
    }

    /**
     * Gets a valid VfsName form a given name in the real filesystem.<p>
     * 
     * This name will ater be used for all link translations during the HTML-parsing process.
     * @param relativeName the name in the real fielsystem, relative to the start folder
     * @param name the name of the file
     * @param isFile flag to indicate that the resource is a file
     * 
     * @return a valid name in the VFS
     * 
     * @throws Exception if something goes wrong
     */
    private String getVfsName(String relativeName, String name, boolean isFile) throws Exception {

        // first translate all fileseperators to the valid "/" in OpenCms
        String vfsName = relativeName.replace('\\', '/');
        // the resource is a file
        if (isFile) {
            // we must check if it might be copied into a gallery. this can be done by checking the
            // file extension
            int filetype = getFileType(name);

            // there is no name before the ".extension"
            if (name.indexOf(".") == 0) {
                name = "unknown" + name;
                int dot = relativeName.lastIndexOf(".");

                relativeName = relativeName.substring(0, dot) + name;
            }

            // depending on the filetype, the resource must be moved into a special folder in 
            // OpenCms:
            // images -> move into image gallery, if flag to leave at original location is off
            // binary -> move into download gallery, if flag to leaave at original location is off
            // plain -> move into destination folder
            // other -> move into download gallery, if flag to leaave at original location is off
            if ((CmsResourceTypeImage.getStaticTypeId() == filetype) && (!m_leaveImages)) {
                // move to image gallery
                // as the image gallery is "flat", we must use the file name and not the complete
                // relative name
                vfsName = m_imageGallery + name;
            } else if ((CmsResourceTypePlain.getStaticTypeId() == filetype) || (m_leaveImages) || (m_leaveDownloads)) {
                // move to destination folder
                //vfsName=m_destinationDir+relativeName;

                // we have to check if there is a folder with the same name but without extension
                // if so, we will move the file into the folder and name it "index.html"
                String folderName = relativeName;
                if (folderName.indexOf(".") > 0) {
                    folderName = folderName.substring(0, folderName.indexOf("."));
                }
                folderName = m_inputDir + "\\" + folderName;
                File folder = new File(folderName);

                if (folder.isDirectory()) {
                    vfsName = m_destinationDir + relativeName.substring(0, relativeName.indexOf(".")) + "/index.html";
                    // System.err.println("MOVING "+ relativeName + " -> " + name.substring(0,name.indexOf("."))+"/index.html");
                } else {
                    // move to destination folder
                    vfsName = m_destinationDir + relativeName;
                }

            } else {
                // everything else will be moved to the download gallery.
                // as the download gallery is "flat", we must use the file name and not the complete
                // relative name
                vfsName = m_downloadGallery + name;
            }
            // now we have the filename in the VFS. its possible that a file with the same name
            // is already existing, in this case, we have to adjust the filename.        
            return validateFilename(vfsName);
        } else {
            // folders are always moved to the destination folder
            vfsName = m_destinationDir + vfsName + "/";
            return vfsName;
        }
    }

    /**
     * Tests if a filename is an external name, i.e. this name does not point into the OpenCms Vfs.<p>
     * 
     * A filename is an external name if it contains the string "://", e.g. "http://" or "ftp://"
     * @param filename the filename to test 
     * @return true or false
     */
    private boolean isExternal(String filename) {

        boolean external = false;
        if (filename.indexOf("://") > 0) {
            external = true;
        }
        return external;
    }

    /** 
     * Checks if m_element is valid element.<p>
     * 
     * @return true if element is valid, otherwise false
     */
    private boolean isValidElement() {

        boolean validElement = false;
        List elementList = new ArrayList();
        try {
            // get Elements of template stored in Property "template-elements"
            String elements = m_cmsObject.readPropertyObject(m_template, CmsPropertyDefinition.PROPERTY_TEMPLATE_ELEMENTS, false).getValue();
            // template may contain more than one Element
            // Elements are seperated by the delimiter ","
            if (elements != null) {
                StringTokenizer T = new StringTokenizer(elements, ",");
                while (T.hasMoreTokens()) {
                    // current element probably looks like "body*|Body" <name><mandatory>|<nicename>
                    String currentElement = T.nextToken();
                    int sepIndex = currentElement.indexOf("|");
                    if (sepIndex != -1) {
                        // current element == "body*"
                        currentElement = currentElement.substring(0, sepIndex);
                    }
                    if (currentElement.endsWith("*")) {
                        // current element == "body"
                        currentElement = currentElement.substring(0, currentElement.length() - 1);
                    }
                    elementList.add(currentElement);
                }
            }
            if (elementList.contains(m_element)) {
                validElement = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return validElement;
    }

    /**
     * Reads the content of an Html file from the real file system and parses it for link
     * transformation.<p>
     * 
     * @param  file the filein the real file system
     * @param properties the file properties
     * @return the modified Html code of the file
     * @throws CmsException if something goes wrong
     */
    private String parseHtmlFile(File file, Hashtable properties) throws CmsException {

        String parsedHtml = "";
        try {

            byte[] content = getFileBytes(file);

            // use the correct encoding to get the string from the file bytes
            String contentString = new String(content, m_inputEncoding);
            // escape the string to remove all special chars
            contentString = CmsEncoder.escapeNonAscii(contentString);
            // we must substitute all occurences of "&#", otherwiese tidy would remove them
            contentString = CmsStringUtil.substitute(contentString, "&#", "{subst}");
            // parse the content                  
            parsedHtml = m_htmlConverter.convertHTML(
                file.getAbsolutePath(),
                contentString,
                m_startPattern,
                m_endPattern,
                properties);
            // resubstidute the converted HTML code
            parsedHtml = CmsStringUtil.substitute(parsedHtml, "{subst}", "&#");
        } catch (Exception e) {
            CmsMessageContainer message = Messages.get().container(
                Messages.ERR_HTMLIMPORT_PARSE_1,
                file.getAbsolutePath());
            LOG.error(e.getLocalizedMessage(), e);
            throw new CmsImportExportException(message, e);
        }
        return parsedHtml;
    }

    /**
     * Validates a fielname for OpenCms.<p>
     * 
     * This method checks if there are any illegal characters in the fielname and modifies them
     * if nescessary. In addition it ensures that no dublicate filenames are created.
     * 
     * @param filename the filename to validate
     * @return a validated and unique filename in OpenCms
     */
    private String validateFilename(String filename) {

        // if its an external filename, use it directley        
        if (isExternal(filename)) {
            return filename;
        }

        // check if this resource name does already exist
        // if so add a postfix to the name

        int postfix = 1;
        boolean found = true;
        String validFilename = filename;

        // if we are not in overwrite mode, we must find a valid, non-existing filename
        // otherwise we will use the current translated name
        if (!m_overwrite) {

            while (found) {
                try {
                    // get the translated name, this one only contains valid chars in OpenCms       
                    validFilename = m_cmsObject.getRequestContext().getFileTranslator().translateResource(validFilename);

                    // try to read the file.....
                    found = true;
                    // first try to read it form the fileIndex of already processed files
                    if (!m_fileIndex.containsValue(validFilename.replace('\\', '/'))) {
                        found = false;
                    }
                    if (!found) {
                        found = true;
                        // there was no entry in the fileIndex, so try to read from the VFS         
                        m_cmsObject.readResource(validFilename, CmsResourceFilter.ALL);
                    }
                    // ....it's there, so add a postfix and try again
                    String path = filename.substring(0, filename.lastIndexOf("/") + 1);
                    String name = filename.substring(filename.lastIndexOf("/") + 1, filename.length());
                    validFilename = path;
                    if (name.lastIndexOf(".") > 0) {
                        validFilename += name.substring(0, name.lastIndexOf("."));
                    } else {
                        validFilename += name;
                    }
                    validFilename += "_" + postfix;
                    if (name.lastIndexOf(".") > 0) {
                        validFilename += name.substring(name.lastIndexOf("."), name.length());
                    }
                    postfix++;
                } catch (CmsException e) {
                    // the file does not exist, so we can use this filename                               
                    found = false;
                }
            }

        } else {
            validFilename = validFilename.replace('\\', '/');
        }

        return OpenCms.getResourceManager().getFileTranslator().translateResource(validFilename);
    }

}
