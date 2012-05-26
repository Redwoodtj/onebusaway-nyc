package org.onebusaway.nyc.webapp.actions.admin.bundles;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.admin.model.BundleBuildRequest;
import org.onebusaway.nyc.admin.model.BundleBuildResponse;
import org.onebusaway.nyc.admin.model.BundleRequest;
import org.onebusaway.nyc.admin.model.BundleResponse;
import org.onebusaway.nyc.admin.model.ui.ExistingDirectory;
import org.onebusaway.nyc.admin.service.BundleRequestService;
import org.onebusaway.nyc.admin.service.FileService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;

import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Action class that holds properties and methods required across all bundle building UI pages
 * @author abelsare
 * @author sheldonabrown
 *
 */
@Namespace(value="/admin/bundles")
@Results({
    @Result(type = "redirectAction", name = "redirect", params = {
    "actionName", "manage-bundles"}),
    @Result(name="selectDirectory", type="json", 
  params={"root", "bundleDirectory"}),
    @Result(name="validationResponse", type="json", 
  params={"root", "bundleResponse"}),
    @Result(name="buildResponse", type="json", 
  params={"root", "bundleBuildResponse"})
})
public class ManageBundlesAction extends OneBusAwayNYCAdminActionSupport {
  private static Logger _log = LoggerFactory.getLogger(ManageBundlesAction.class);
	private static final long serialVersionUID = 1L;
	//To hold the final directory name 
	private String bundleDirectory;
	private String createDirectoryMessage;
	//Holds the value entered in the text box
	private String directoryName;
	// what to call the bundle, entered in the text box
	private String bundleName;
	private boolean directoryCreated;
	private boolean productionTarget;
	private String comments;
	private FileService fileService;
	private BundleRequestService bundleRequestService;
	private static final int MAX_RESULTS = -1;
	private BundleResponse bundleResponse;
	private BundleBuildResponse bundleBuildResponse;
	private String id;
	
	@Override
	public String input() {
	  _log.debug("in input");
    return SUCCESS;
	  
	}
	
	@Override
	public String execute() {
	  _log.info("in execute");
	  return SUCCESS;
	}
	
	/**
	 * Creates directory for uploading bundles on AWS
	 */
	public String createDirectory() {
	  _log.debug("in create directory with dir=" + directoryName);
		if(fileService.bundleDirectoryExists(directoryName)) {
		  _log.info("bundle dir exists");
			createDirectoryMessage = directoryName + " already exists. Please try again!";
		} else {
		  _log.info("creating bundledir");
			//Create the directory if it does not exist.
			directoryCreated = fileService.createBundleDirectory(directoryName);
			selectDirectory();
			if(directoryCreated) {
				createDirectoryMessage = "Successfully created new directory: " +directoryName;
			} else {
				createDirectoryMessage = "Unable to create direcory: " +directoryName;
				return INPUT;
			}
		}
		return "prevalidate";
	}
	
	public String selectDirectory() {
	  _log.info("in selectDirectory with dirname=" + directoryName);
	  bundleDirectory = directoryName;
	  return "selectDirectory";
	}
	/**
	 * Returns the existing directories in the current bucket on AWS
	 * @return list of existing directories
	 */
	public List<ExistingDirectory> getExistingDirectories() {
	  //TODO optimize this call implementation -- it takes too long
		List<String[]> existingDirectories = fileService.listBundleDirectories(MAX_RESULTS);
		List<ExistingDirectory> directories = new ArrayList<ExistingDirectory>();
		for(String[] existingDirectory : existingDirectories) {
			ExistingDirectory directory = new ExistingDirectory(existingDirectory[0], existingDirectory[1], 
					existingDirectory[2]);
			directories.add(directory);
		}
		
		return directories;
	}
	
	
	/**
	 * Validates a bundle request and generates a response
	 * @return bundle response as validation result.
	 */
	public String validateBundle() {
	  _log.info("in validateBundle with bundleDirectory=" + bundleDirectory);
		BundleRequest bundleRequest = new BundleRequest();
		bundleRequest.setBundleDirectory(bundleDirectory);
		this.bundleResponse = bundleRequestService.validate(bundleRequest);
		_log.info("id=" + this.bundleResponse.getId());
		_log.info("complete=" + this.bundleResponse.isComplete());
		return "validationResponse";
	}
	
	public String validateStatus() {
	  _log.info("in validateStatus with id=" + getId());
	  this.bundleResponse = bundleRequestService.lookupValidationRequest(getId());
	  return "validationResponse";
	}

	
	public String buildBundle() {
	  _log.info("in buildBundle with bundleDirectory=" + bundleDirectory 
	      + " and bundleName=" + bundleName);
		BundleBuildRequest bundleRequest = new BundleBuildRequest();
		bundleRequest.setBundleDirectory(bundleDirectory);
		bundleRequest.setBundleName(bundleName);
		//bundleRequest.
		this.bundleBuildResponse = bundleRequestService.build(bundleRequest);
		_log.info("id=" + this.bundleBuildResponse.getId());
		_log.info("complete=" + this.bundleBuildResponse.isComplete());
		return "buildResponse";
	}
	
	public String buildStatus() {
	  _log.info("in validateStatus with id=" + getId());
	  this.bundleBuildResponse = bundleRequestService.lookupBuildRequest(getId());
	  return "buildResponse";
	}
	
//	/**
//	 * Stores the newly created or selected directory name from the UI.
//	 */
//	public void selectDirectory() {
//		bundleDirectory = directoryName;
//	}
	
	/**
	 * @return the createDirectoryMessage
	 */
	public String getCreateDirectoryMessage() {
		return createDirectoryMessage;
	}
	
	/**
	 * @param createDirectoryMessage the createDirectoryMessage to set
	 */
	public void setCreateDirectoryMessage(String createDirectoryMessage) {
		this.createDirectoryMessage = createDirectoryMessage;
	}
	
	/**
	 * @return the directoryName
	 */
	public String getDirectoryName() {
		return directoryName;
	}
	
	/**
	 * @param directoryName the directoryName to set
	 */
	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}
	
	/**
	 * @return the directoryCreated
	 */
	public boolean isDirectoryCreated() {
		return directoryCreated;
	}
	
	/**
	 * @param directoryCreated the directoryCreated to set
	 */
	public void setDirectoryCreated(boolean directoryCreated) {
		this.directoryCreated = directoryCreated;
	}
	
	/**
	 * @return the productionTarget
	 */
	public boolean isProductionTarget() {
		return productionTarget;
	}
	
	/**
	 * @param productionTarget the productionTarget to set
	 */
	public void setProductionTarget(boolean productionTarget) {
		this.productionTarget = productionTarget;
	}
	
	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}
	
	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	/**
	 * @param fileService the fileService to set
	 */
	@Autowired
	public void setFileService(FileService fileService) {
		this.fileService = fileService;
	}
	
	/**
	 * @return the bundleDirectory
	 */
	public String getBundleDirectory() {
		return bundleDirectory;
	}
	
	/**
	 * @param bundleDirectory the bundleDirectory to set
	 */
	public void setBundleDirectory(String bundleDirectory) {
		this.bundleDirectory = bundleDirectory;
	}
	

	/**
	 * Injects {@link BundleRequestService}
	 * @param bundleRequestService the bundleRequestService to set
	 */
	@Autowired
	public void setBundleRequestService(BundleRequestService bundleRequestService) {
		this.bundleRequestService = bundleRequestService;
	}

	public BundleResponse getBundleResponse() {
	  return bundleResponse;
	}

	public BundleBuildResponse getBundleBuildResponse() {
	  return bundleBuildResponse;
	}
	
	public void setId(String id) {
	  this.id = id;
	}
	
	public String getId() {
	    return id;
	}
	
	public void setBundleName(String bundleName) {
	  this.bundleName = bundleName;
	}
	
	public String getBundleName() {
    return bundleName;
	}
}
