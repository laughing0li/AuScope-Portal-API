package org.auscope.portal.server.web.controllers;

import java.util.ArrayList;

import net.sf.json.JSONArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.auscope.portal.csw.CSWOnlineResource;
import org.auscope.portal.csw.CSWRecord;
import org.auscope.portal.csw.CSWOnlineResource.OnlineResourceType;
import org.auscope.portal.server.util.PortalPropertyPlaceholderConfigurer;
import org.auscope.portal.server.web.KnownFeatureTypeDefinition;
import org.auscope.portal.server.web.service.CSWService;
import org.auscope.portal.server.web.view.JSONModelAndView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @version $Id$
 */
@Controller
public class CSWController {

    protected final Log log = LogFactory.getLog(getClass());
    private CSWService cswService;
    private ArrayList<KnownFeatureTypeDefinition> knownTypes;

    /**
     * Construct
     * @param
     */
    @Autowired
    public CSWController(CSWService cswService,
                         PortalPropertyPlaceholderConfigurer portalPropertyPlaceholderConfigurer,
                         ArrayList<KnownFeatureTypeDefinition> knownTypes) {

        this.cswService = cswService;
        this.knownTypes = knownTypes;

        String cswServiceUrl =
            portalPropertyPlaceholderConfigurer.resolvePlaceholder("HOST.cswservice.url");
        log.debug("cswServiceUrl: " + cswServiceUrl);
        cswService.setServiceUrl(cswServiceUrl);

        try {
            cswService.updateRecordsInBackground();
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * This controller queries a CSW for all of its WFS data records based on known feature types, then created a JSON response as a list
     * which can then be put into a table.
     *
     * Returns a JSON response with a data structure like so
     *
     * [
     * [title, description, [contactOrganisations], proxyURL, serviceType, id, typeName, [serviceURLs], checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage],
     * [title, description, [contactOrganisations], proxyURL, serviceType, id, typeName, [serviceURLs], checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage]
     * ]
     *
     * @return
     */
    @RequestMapping("/getComplexFeatures.do")
    public ModelAndView getComplexFeatures() throws Exception {

        //update the records if need be
        cswService.updateRecordsInBackground();

        //the main holder for the items
        JSONArray dataItems = new JSONArray();

        log.debug("WFS Layers: ");
        for(Object known : knownTypes) {

            KnownFeatureTypeDefinition knownType = (KnownFeatureTypeDefinition)known;

            log.debug("KnownType: " + knownType.getFeatureTypeName() + " --> " + knownType.getDisplayName());

            //Add the mineral occurrence
            JSONArray tableRow = new JSONArray();
            JSONArray contactOrgs = new JSONArray();

            //add the name of the layer/feature type
            tableRow.add(knownType.getDisplayName());

            CSWRecord[] records = cswService.getWFSRecordsForTypename(knownType.getFeatureTypeName());
            String servicesDescription = "Institutions: ";
            JSONArray serviceURLs = new JSONArray();
            JSONArray bboxes = new JSONArray();

            //if there are no services available for this feature type then don't show it in the portal
            if(records.length == 0) {                
                continue;
            }

            for(CSWRecord record : records) {
                for (CSWOnlineResource resource : record.getOnlineResourcesByType(OnlineResourceType.WFS)) {
                    log.debug("...registered service: " + resource.getLinkage().toString());
                    serviceURLs.add(resource.getLinkage().toString());
                }
                
                servicesDescription += record.getContactOrganisation() + ", ";
                contactOrgs.add(record.getContactOrganisation());

                if (record.getCSWGeographicElement() != null)
                    bboxes.add(record.getCSWGeographicElement());
            }

            //add the abstract text to be shown as updateCSWRecords description
            tableRow.add(knownType.getDescription() + " " + servicesDescription);
            
            //Add the list of contact organisations
            tableRow.add(contactOrgs);

            //add the service URL - this is the spring controller for handling minocc
            tableRow.add(knownType.getProxyUrl());

            //add the type: wfs or wms
            tableRow.add("wfs");

            //TODO: add updateCSWRecords proper unique id
            tableRow.add(knownType.hashCode());

            //add the featureType name (in case of updateCSWRecords WMS feature)
            tableRow.add(knownType.getFeatureTypeName());

            tableRow.add(serviceURLs);

            tableRow.element(true);
            tableRow.add("<img src='js/external/extjs/resources/images/default/grid/done.gif'>");

            tableRow.add("<img width='16' heigh='16' src='" + knownType.getIconUrl() + "'>");
            tableRow.add(knownType.getIconUrl());

            tableRow.add("<a href='http://portal.auscope.org' id='mylink' target='_blank'><img src='img/page_code.png'></a>");

            tableRow.add(bboxes);

            dataItems.add(tableRow);
        }
        
        log.debug("\n" + dataItems.toString());
        return new JSONModelAndView(dataItems);
    }

    /**
     * Gets all WMS data records from a CSW service, and then creats a JSON response for the WMS layers list in the portal
     *
     * Returns a JSON response with a data structure like so
     *
     * [
     * [title, description, contactOrganisation, proxyURL, serviceType, id, typeName, [serviceURLs], checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage, opacity],
     * [title, description, contactOrganisation, proxyURL, serviceType, id, typeName, [serviceURLs], checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage, opacity]
     * ]
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/getWMSLayers.do")
    public ModelAndView getWMSLayers() throws Exception {
        //update the records if need be
        cswService.updateRecordsInBackground();

        //the main holder for the items
        JSONArray dataItems = new JSONArray();

        CSWRecord[] records = cswService.getWMSRecords();

        for(CSWRecord record : records) {
            for (CSWOnlineResource wmsResource : record.getOnlineResourcesByType(OnlineResourceType.WMS)) {
                
                //Add the mineral occurrence
                JSONArray tableRow = new JSONArray();
    
                //add the name of the layer/feature type
                tableRow.add(record.getServiceName());
    
                //add the abstract text to be shown as updateCSWRecords description
                tableRow.add(record.getDataIdentificationAbstract());
                
                //Add the contact organisation
                String org = record.getContactOrganisation();
                if (org == null || org.length() == 0)
                	org = "Unknown";
                tableRow.add(org);
    
                //wms dont need updateCSWRecords proxy url
                tableRow.add("");
    
                //add the type: wfs or wms
                tableRow.add("wms");
    
                //TODO: add updateCSWRecords proper unique id
                tableRow.add(record.hashCode());
    
                //add the featureType name (in case of updateCSWRecords WMS feature))
                tableRow.add(wmsResource.getName());
    
                JSONArray serviceURLs = new JSONArray();
    
                serviceURLs.add(wmsResource.getLinkage().toString());
    
                tableRow.add(serviceURLs);
    
                tableRow.element(true);
                tableRow.add("<img src='js/external/extjs/resources/images/default/grid/done.gif'>");
    
                tableRow.add("<a href='http://portal.auscope.org' id='mylink' target='_blank'><img src='img/picture_link.png'></a>");
                
                tableRow.add("1.0");
                
                JSONArray bboxes = new JSONArray();
                if (record.getCSWGeographicElement() != null)
                    bboxes.add(record.getCSWGeographicElement());
                
                tableRow.add(bboxes);
    
                dataItems.add(tableRow);
            }
        }
        log.debug(dataItems.toString());
        return new JSONModelAndView(dataItems);
    }
    
    /**
     * Gets all WCS data records from a CSW service, and then creates a JSON response for the WCS layers list in the portal
     *
     * Returns a JSON response with a data structure like so
     *
     * [
     * [title, description, contactOrganisation, proxyURL, serviceType, id, typeName, [serviceURLs], [openDapUrls] , checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage, [bboxes]],
     * [title, description, contactOrganisation, proxyURL, serviceType, id, typeName, [serviceURLs], [openDapUrls] , checked, statusImage, markerIconHtml, markerIconUrl, dataSourceImage, [bboxes]]
     * ]
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/getWCSLayers.do")
    public ModelAndView getWCSLayers() throws Exception {
        //update the records if need be
        cswService.updateRecordsInBackground();

        //the main holder for the items
        JSONArray dataItems = new JSONArray();

        CSWRecord[] records = cswService.getWCSRecords();

        for(CSWRecord record : records) {
            for (CSWOnlineResource wcsResource : record.getOnlineResourcesByType(OnlineResourceType.WCS)) {
                //Add the mineral occurrence
                JSONArray tableRow = new JSONArray();
    
                //add the name of the layer/feature type
                tableRow.add(record.getServiceName());
    
                //add the abstract text to be shown as updateCSWRecords description
                tableRow.add(record.getDataIdentificationAbstract());
                
                //Add the contact organisation
                String org = record.getContactOrganisation();
                if (org == null || org.length() == 0)
                    org = "Unknown";
                tableRow.add(org);
    
                //wcs dont need updateCSWRecords proxy url
                tableRow.add("");
    
                //add the type: wfs or wms or wcs
                tableRow.add("wcs");
    
                //TODO: add updateCSWRecords proper unique id
                tableRow.add(record.hashCode());
    
                //add the featureType name (in case of updateCSWRecords WMS feature)
                tableRow.add(wcsResource.getName());
    
                JSONArray serviceURLs = new JSONArray();
                serviceURLs.add(wcsResource.getLinkage().toString());
                tableRow.add(serviceURLs);
                
                //This is currently a hack so we can piggy back open DAP onto WCS
                JSONArray openDapURLs = new JSONArray();
                for (CSWOnlineResource openDapResource : record.getOnlineResourcesByType(OnlineResourceType.OpenDAP)) {
                    openDapURLs.add(openDapResource.getLinkage().toString());
                }
                tableRow.add(openDapURLs);
    
                tableRow.element(true);
                tableRow.add("<img src='js/external/extjs/resources/images/default/grid/done.gif'>");
    
                tableRow.add("<a href='http://portal.auscope.org' id='mylink' target='_blank'><img src='img/picture_link.png'></a>");
                
                JSONArray bboxes = new JSONArray();
                if (record.getCSWGeographicElement() != null)
                    bboxes.add(record.getCSWGeographicElement());
                
                tableRow.add(bboxes);
    
                dataItems.add(tableRow);
            }
        }
        log.debug(dataItems.toString());
        return new JSONModelAndView(dataItems);
    }
}