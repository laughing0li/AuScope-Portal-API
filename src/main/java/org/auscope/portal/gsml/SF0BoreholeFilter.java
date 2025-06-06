package org.auscope.portal.gsml;

import java.util.ArrayList;
import java.util.List;

import org.auscope.portal.core.services.methodmakers.filter.FilterBoundingBox;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

/**
 * A class for filter SF0 Borehole web service
 *
 * @author Florence Tan
 *
 */
@Service
public class SF0BoreholeFilter extends BoreholeFilter {
    protected Boolean justNVCL;
    protected List<String> identifiers;
    protected Boolean omitGsmlpShapeProperty = false;
    // ----------------------------------------------------------- Constructors

    public SF0BoreholeFilter() {
        // test
        super(null, null, null, null, null, null);
    }
    
        
    public SF0BoreholeFilter(Boolean justNVCL) {
        // test
        super(null, null, null, null, null, null);
        this.justNVCL = justNVCL;
    }

    public SF0BoreholeFilter(String boreholeName, String custodian, String dateOfDrillingStart, String dateOfDrillingEnd,
    		List<String> ids, List<String> identifiers, Boolean justNVCL, String optionalFilters) {
        super(boreholeName, custodian, dateOfDrillingStart, dateOfDrillingEnd, ids, optionalFilters);
        this.justNVCL = justNVCL;
        this.identifiers = identifiers;
    }
    
    public SF0BoreholeFilter(String boreholeName, String custodian, String dateOfDrillingStart, String dateOfDrillingEnd,
    		List<String> ids, List<String> identifiers, Boolean justNVCL, String optionalFilters, Boolean omitGsmlpShapeProperty) {
        super(boreholeName, custodian, dateOfDrillingStart, dateOfDrillingEnd, ids, optionalFilters);
        this.justNVCL = justNVCL;
        this.identifiers = identifiers;
        this.omitGsmlpShapeProperty = omitGsmlpShapeProperty;
    }

    // --------------------------------------------------------- Public Methods

    @Override
    public String getFilterStringAllRecords() {
        return this.generateFilter(this.generateFilterFragment());
    }

    @Override
    public String getFilterStringBoundingBox(FilterBoundingBox bbox) {
    	
        return this
                .generateFilter(this.generateAndComparisonFragment(
                        this.generateBboxFragment(bbox,
                                omitGsmlpShapeProperty ? null : "gsmlp:shape"),
                                this.generateFilterFragment()));
    }

    @Override
    protected String generateFilterFragment() {
        List<String> parameterFragments = null;
        String optionalFilters = this.getxPathFilters();
        if(optionalFilters == null || optionalFilters.isEmpty()){
            parameterFragments = new ArrayList<String>();
            if (boreholeName != null && !boreholeName.isEmpty()) {
                parameterFragments.add(this.generatePropertyIsLikeFragment(
                        "gsmlp:name", this.boreholeName));
            }

            if (custodian != null && !custodian.isEmpty()) {
                parameterFragments
                .add(this
                        .generatePropertyIsLikeFragment(
                                "gsmlp:boreholeMaterialCustodian",
                                this.custodian));
            }

            if (dateOfDrillingStart != null && !dateOfDrillingStart.isEmpty()
                    && dateOfDrillingEnd != null && !dateOfDrillingEnd.isEmpty()) {
                // AUS-2595 Due to the date compare does not like the
                // PropertyIsLike, it was change to use PropertyIsGreaterThan & PropertyIsLessThan.
                DateTimeFormatter formatter = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd");
                LocalDate dStart = LocalDate
                        .parse(this.dateOfDrillingStart, formatter);
                LocalDate dEnd = LocalDate.parse(this.dateOfDrillingEnd, formatter);
                // LJ: Need to minus 1 second for startDate to cover the time of
                // 00:00:00
                // Need to plus 1 second for endDate to cover the time of 00:00:00
                LocalDateTime dtStart = dStart.atTime(0,0);
                dtStart = dtStart.minusSeconds(1);
                LocalDateTime dtEnd = dEnd.atTime(0,0);
                dtEnd = dtEnd.plusSeconds(1);
                DateTimeFormatter outFormatter = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss");
                String utcDateofDrillingStart = outFormatter.format(dtStart);
                String utcDateofDrillingEnd = outFormatter.format(dtEnd);
                parameterFragments.add(this.generateDatePropertyIsGreaterThan(
                        "gsmlp:drillStartDate",false,
                        this.generateFunctionDateParse(utcDateofDrillingStart)));

                parameterFragments.add(this.generateDatePropertyIsLessThan(
                        "gsmlp:drillStartDate",false,
                        this.generateFunctionDateParse(utcDateofDrillingEnd)));
            }
        }else{
            parameterFragments = this.generateParameterFragments();
        }

        if (this.restrictToIDList != null && !this.restrictToIDList.isEmpty()) {
            List<String> idFragments = new ArrayList<String>();
            for (String id : restrictToIDList) {
                if (id != null && id.length() > 0) {
                    idFragments.add(this.generateFeatureIdFragment("gsml.borehole." + id));
                }
            }
            parameterFragments.add(this
                    .generateOrComparisonFragment(idFragments
                            .toArray(new String[idFragments.size()])));
        }

        if (this.identifiers != null && !this.identifiers.isEmpty()) {
            List<String> compareFragments = new ArrayList<String>();
            for (String identifier : identifiers) {
                if (identifier != null && identifier.length() > 0) {
                    compareFragments.add(this.generatePropertyIsEqualToFragment("gsmlp:identifier", identifier));
                }
            }

            parameterFragments.add(this
                    .generateOrComparisonFragment(compareFragments
                            .toArray(new String[compareFragments.size()])));
        }


        if (this.justNVCL != null && this.justNVCL==true) {
            // We can "optimise" the query if we are using "justNVCL" boreholes.
            // NB: 'matchCase' isn't supported by all geoserver installations, hence have to use the 'OR' operator
            // NB: the Or operator causes problems for newer geoservers 2.18.2 and above. fortunately, all NVCL layers now use lower case 'true' 
            return this.generateAndComparisonFragment(this.generatePropertyIsEqualToFragment("gsmlp:nvclCollection", "true", false),
                    this.generateAndComparisonFragment(parameterFragments.toArray(new String[parameterFragments.size()])));
        } else {
            return this.generateAndComparisonFragment(parameterFragments.toArray(new String[parameterFragments.size()]));
        }

    }
}
