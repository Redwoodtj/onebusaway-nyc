
/**
 * Copyright (C) 2015 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.admin.service.bundle.task.gtfsTransformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.onebusaway.nyc.admin.model.BundleRequestResponse;
import org.onebusaway.nyc.admin.util.FileUtils;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs_transformer.GtfsTransformer;
import org.onebusaway.gtfs_transformer.GtfsTransformerLibrary;
import org.onebusaway.gtfs_transformer.factory.TransformFactory;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.onebusaway.nyc.util.impl.FileUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;


public class BaseModTask {
    private static Logger _log = LoggerFactory.getLogger(BaseModTask.class);
    protected ApplicationContext _applicationContext;
    protected MultiCSVLogger logger;
    protected BundleRequestResponse requestResponse;
    private String _directoryHint = "modified";
    protected ConfigurationService configurationService;
    private String[] REQUIRED_GTFS_FILES = {"agency.txt","routes.txt","stops.txt","calendar.txt","shapes.txt","trips.txt","stop_times.txt"};
    private boolean ARG_WRITE_ZONE_ROUTE_MAPPING = false;
    private String ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        _applicationContext = applicationContext;
    }

    @Autowired
    public void setBundleRequestResponse(BundleRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    @Autowired
    public void setLogger(MultiCSVLogger logger) {
        this.logger = logger;
    }

    public void setDirectoryHint(String hint) {
        _directoryHint = hint;
    }

    public void setWriteZoneRouteMapping(boolean shouldMap){
        ARG_WRITE_ZONE_ROUTE_MAPPING = shouldMap;
    }

    public void setRouteMappingOutputName(String routeMappingOutputName){
        ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME = routeMappingOutputName;
    }

    public String getRouteMappingOutputName() {
        return ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME;
    }

    public boolean getWriteZoneRouteMapping() {
        return ARG_WRITE_ZONE_ROUTE_MAPPING;
    }


    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected String getEmptyModUrl() {
        return "https://raw.githubusercontent.com/wiki/camsys/onebusaway-application-modules/EmptyModifications.md";
    }

    protected String runModifications(GtfsBundle gtfsBundle, String agencyId,
                                      String modPath, String transform) throws Exception {
        if (skipMod(agencyId)) {
            _log.info("runModifications skipping agencyId " + agencyId + " as multiAgency set to false");
            return gtfsBundle.getPath().getPath();
        }
        _log.info("runModifications(" + agencyId + ") with mappings=" + gtfsBundle.getAgencyIdMappings() );
        GtfsTransformer mod = new GtfsTransformer();

        TransformFactory factory = mod.getTransformFactory();
        // the transformer may be called twice causing erroneous duplicate messages
        mod.getReader().setOverwriteDuplicates(true);

        mod.setWriteZoneRouteMapping(ARG_WRITE_ZONE_ROUTE_MAPPING);
        mod.setRouteMappingOutputName(ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME);
        addAgencyMappings(mod.getReader(), gtfsBundle);

        String outputDirectory = parseDirectory(gtfsBundle.getPath().getPath());

        List<File> paths = new ArrayList<File>();
        paths.add(gtfsBundle.getPath());
        _log.info("transformer path=" + gtfsBundle.getPath() + "; output="
                + outputDirectory + " for modPath=" + modPath);
        mod.setGtfsInputDirectories(paths);
        mod.setOutputDirectory(new File(outputDirectory));
        GtfsTransformerLibrary.configureTransformation(mod, modPath);
        String path = gtfsBundle.getPath().getPath();
        if (transform != null) {
            _log.info("using transform=" + transform);
            factory.addModificationsFromString(transform);
        }

        addExtraMods(mod);
        _log.info("running...");
        mod.run();
        _log.info("done!");
        // cleanup
        return cleanup(gtfsBundle);
    }

    public void addExtraMods(GtfsTransformer mod) {
        // default: none
    }

    private boolean skipMod(String agencyId) {
        try {
            return "false".equals(configurationService.getConfigurationValueAsString(agencyId+"_multiAgency",""));
        } catch (Exception e) {}
        return false;
    }

    private void addAgencyMappings(GtfsReader reader, GtfsBundle gtfsBundle) {
        if (gtfsBundle != null && gtfsBundle.getAgencyIdMappings() != null) {
            for (String key : gtfsBundle.getAgencyIdMappings().keySet()) {
                reader.addAgencyIdMapping(key, gtfsBundle.getAgencyIdMappings().get(key));
            }
        }
    }

    private String cleanup(GtfsBundle gtfsBundle) throws Exception {

        File gtfsFile = gtfsBundle.getPath();
        FileUtility fu = new FileUtility();
        FileUtils fs = new FileUtils();




        _log.info("gtfsBundle.getPath=" + gtfsFile.getPath());
        String oldGtfsName = gtfsFile.getPath().toString();
        // delete the old zip file
        _log.info("deleting " + gtfsFile.getPath());
        gtfsFile.delete();
        // create a new zip file

        String newGtfsName = fs.parseDirectory(oldGtfsName) + File.separator
                + fs.parseFileNameMinusExtension(oldGtfsName) + "_mod.zip";

        String basePath = fs.parseDirectory(oldGtfsName);
        if(ARG_WRITE_ZONE_ROUTE_MAPPING) {
            handleRoutesByZone(basePath);
        }
        String includeExpression = ".*\\.txt";
        testForMissingFiles(basePath, gtfsBundle.getPath().getName());
        fu.zip(newGtfsName, basePath, includeExpression);
        int deletedFiles = fu.deleteFilesInFolder(basePath, includeExpression);
        if (deletedFiles < 1) {
            throw new IllegalStateException(
                    "Missing expected modded gtfs files in directory " + basePath);
        }

        gtfsBundle.setPath(new File(newGtfsName));
        _log.info("gtfsBundle.getPath(mod)=" + gtfsBundle.getPath() + " with mappings= " + gtfsBundle.getAgencyIdMappings());

        if (getOutputDirectory() != null) {
            File outputDir = new File(getOutputDirectory() + File.separator
                    + getDirectoryHint());
            if (!outputDir.exists() || !outputDir.isDirectory()) {
                outputDir.mkdirs();
            }

            String outputLocation = getOutputDirectory() + File.separator
                    + getDirectoryHint() + File.separator
                    + fs.parseFileName(newGtfsName).replaceAll("_mod", "");
            // copy to outputs for downstream systems
            FileUtils.copyFile(new File(newGtfsName), new File(outputLocation));
        }
        return newGtfsName;
    }

    private void testForMissingFiles(String basePath, String gtfsName) throws InterruptedException {
        File basePathDir = new File(basePath);
        List<String> txtFiles = Arrays.asList(basePathDir.list());
        String somethingBroke = "";
        for (String file : REQUIRED_GTFS_FILES){
            if(!txtFiles.contains(file)){
                somethingBroke += file + " ";
            }
        }
        if(!somethingBroke.equals("")){
            throw new InterruptedException("Transformer was run on " + gtfsName + ". "
                    + "These transformations removed key fields and resulted in the following empty files:" + somethingBroke +
                    ". Most likely cause: Service Calendar ends before current date, or empty GTFS .txt input files."+
                    " Check transformations for more in depth investigation");
        }

    }


    private void handleRoutesByZone(String outLocation){
        File routesByZone = new File (outLocation + "/" +ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME);
        routesByZone.renameTo(new File(routesByZone.getParentFile().getParentFile().getAbsolutePath()+ "/" +ARG_ROUTE_MAPPING_OUTPUT_FILE_NAME));
    }

    private String getDirectoryHint() {
        return _directoryHint;
    }

    protected String getOutputDirectory() {
        if (this.requestResponse != null && this.requestResponse.getResponse() != null)
            return this.requestResponse.getResponse().getBundleOutputDirectory();
        return null;
    }

    protected String parseAgencyDir(String path) {
        String agency = null;
        int lastSlash = path.lastIndexOf(File.separatorChar);
        if (lastSlash < 0)
            return agency;
        int firstBar = path.indexOf('_', lastSlash);
        if (firstBar < 0)
            return agency;

        return path.substring(lastSlash + 1, firstBar);
    }

    protected String parseDirectory(String path) {
        int lastSlash = path.lastIndexOf(File.separatorChar);
        if (lastSlash < 0)
            return null;

        return path.substring(0, lastSlash);
    }

    protected GtfsBundles getGtfsBundles(ApplicationContext context) {

        GtfsBundles bundles = (GtfsBundles) context.getBean("gtfs-bundles");
        if (bundles != null)
            return bundles;

        GtfsBundle bundle = (GtfsBundle) context.getBean("gtfs-bundle");
        if (bundle != null) {
            bundles = new GtfsBundles();
            bundles.getBundles().add(bundle);
            return bundles;
        }

        throw new IllegalStateException(
                "must define either \"gtfs-bundles\" or \"gtfs-bundle\" in config");
    }

}
