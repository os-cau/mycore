/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package mycore.datamodel;

import org.w3c.dom.Node;
import mycore.common.MCRException;

/**
 * This interface is designed to to have a general description of the
 * common methode set of all metadata classes.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public interface MCRMetaInterface
{

/**
 * This methode set the default language to the class.
 *
 * @param default_lang           the default language
 **/
public void setLang(String default_lang);

/**
 * This methode read the XML input stream part from a DOM part for the
 * metadata of the document.
 *
 * @param dom_element_list       a relevant DOM element for the metadata
 **/
public void setFromDOM(Node metadata_langtext_node);

/**
 * This methode create a XML stream for a metadata part.
 *
 * @exception MCRException if the content of this class is not valid
 * @return a XML string with the XML data of the metadata part
 **/
public String createXML() throws MCRException;

/**
 * This methode create a Text Search stream for a metadata part.
 * The content of this stream is depended by the implementation for
 * the persistence database. It was choose over the
 * <em>MCR.persistence_type</em> configuration.
 *
 * @param type   the type of the persistece system
 * @param tag                the tagname of an element list
 * @exception MCRException if the content of this class is not valid
 * @return a Text Search string with the data of the metadata part
 **/
public String createTS(Object mcr_query,String tag) throws MCRException;

/**
 * This methode check the validation of the content of this class.
 *
 * @return a boolean value
 **/
public boolean isValid();

/**
 * This methode print all elements of the metadata class.
 **/
public void debug();

}

