/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import java.io.*;
import java.util.*;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * This class handles upload of files for miless derivates.
 * 
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @see org.mycore.frontend.fileupload.MCRUploadHandlerMyCoReBase
 * @see org.mycore.frontend.fileupload.MCRUploadHandlerMyCoReInterface
 */
public class MCRUploadHandlerMyCoRe extends MCRUploadHandlerBase implements
        MCRUploadHandlerInterface {

    /**
     * The constructor.
     */
    public MCRUploadHandlerMyCoRe() {
        super();
    }

    /**
     * set the data of MCRUploadHandlerMyCoRe for MyCoRe
     * 
     * @param docId
     *            document to which derivate belongs
     * @param derId
     *            derivate used to add files, if id="0" a new derivate is
     *            created
     * @param mode
     *            "append" add files to derivate, replace old files "replace"
     *            add files to derivate, delete old files "create" add files to
     *            new derivate
     * @param url
     *            when MCRUploadApplet is finished this url will be shown
     */
    public void set(String docId, String derId, String mode, String url) {
        this.url = url;
        logger.debug("MCRUploadHandlerMyCoRe DocID: " + docId + " DerId: "
                + derId + " Mode: " + mode);
        try {
            MCRObjectID oid = new MCRObjectID(docId);
            this.docId = docId;
        } catch (Exception e) {
        }
        try {
            MCRObjectID did = new MCRObjectID(derId);
            this.derId = derId;
            this.mode = mode;
        } catch (Exception e) {
            this.mode = "create";
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    public void startUpload(int numFiles) throws Exception {
        MCRObjectID ID = new MCRObjectID(docId);
        MCRConfiguration config = MCRConfiguration.instance();
        String workdir = config.getString("MCR.editor_" + ID.getTypeId()
                + "_directory", "/");
        dirname = workdir + SLASH + derId;
    }

    /**
     * Message from UploadApplet If you want all files transfered omit this
     * method
     * 
     * @param path
     *            file name
     * @param chechsum
     *            md5 chechsum of of file
     * 
     * @return true transfer file false don't send file
     *  
     */
    public boolean acceptFile(String path, String checksum) throws Exception {
        return true;
    }

    /**
     * Store file in data store
     * 
     * @param path
     *            file name
     * @param in
     *            InputStream belongs to socket, do not close!
     *  
     */
    public void receiveFile(String path, InputStream in) throws Exception {
        // prepare to save
        logger.debug("Upload file path: " + path);

        // convert path
        String fname = path.replace(' ', '_');
        File fdir = null;
        String newdir = dirname;
        StringTokenizer st = new StringTokenizer(fname, "/");
        int i = st.countTokens();
        int j = 0;
        while (j < i - 1) {
            newdir = newdir + SLASH + st.nextToken();
            j++;
            try {
                fdir = new File(newdir);
                if (!fdir.isDirectory()) {
                    fdir.mkdir();
                    logger.debug("Create directory " + newdir);
                }
            } catch (Exception e) {
            }
        }
        String newfile = st.nextToken();

        // store file
        File fout = new File(newdir, newfile);
        try {
            FileOutputStream fouts = new FileOutputStream(fout);
            MCRUtils.copyStream(in, fouts);
            fouts.close();
            logger.info("Data object stored under " + fout.getName());
        } catch (IOException e) {
            logger.error("Can't store the data object " + fout.getName());
        }

        // set mainfile
        if (mainfile.length() == 0) {
            mainfile = fname;
        }
    }

    /**
     * Finish upload, store derivate
     *  
     */
    public void finishUpload() throws Exception {
        // add the mainfile entry
        try {
            MCRDerivate der = new MCRDerivate();
            der.setFromURI(dirname + ".xml");
            if (der.getDerivate().getInternals().getMainDoc().equals("#####")) {
                der.getDerivate().getInternals().setMainDoc(mainfile);
                byte[] outxml = MCRUtils.getByteArray(der.createXML());
                try {
                    FileOutputStream out = new FileOutputStream(dirname
                            + ".xml");
                    out.write(outxml);
                    out.flush();
                } catch (IOException ex) {
                    logger.error(ex.getMessage());
                    logger.error("Exception while store to file " + dirname
                            + ".xml");
                }
            }
        } catch (Exception e) {
        }
    }

}