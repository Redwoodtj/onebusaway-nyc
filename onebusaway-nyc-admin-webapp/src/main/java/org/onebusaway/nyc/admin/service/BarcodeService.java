/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.admin.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service interface to retrieve qr code images from remote server
 * @author abelsare
 *
 */
public interface BarcodeService {

	/**
	 * Gets QR codes for stop ids specified in csv file. This method retrieves QR codes in a batch as
	 * a zip file from remote server
	 * @param dimensions dimensions of the images
	 * @return input stream of zip file containing qr code images
	 */
	public InputStream getQRCodesInBatch(File stopIdFile, int dimensions) throws IOException;
}
