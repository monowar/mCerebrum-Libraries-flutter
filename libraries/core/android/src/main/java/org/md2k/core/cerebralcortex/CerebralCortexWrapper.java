/*
 * Copyright (c) 2018, The University of Memphis, MD2K Center of Excellence
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.md2k.core.cerebralcortex;

import java.io.IOException;

/**
 * Provides a wrapper for <code>CerebralCortex</code> API calls.
 */
public class CerebralCortexWrapper extends Thread {
/*

    */
/**
     * Constant used for logging. <p>Uses <code>class.getSimpleName()</code>.</p>
     *//*

    private static final String TAG = CerebralCortexWrapper.class.getSimpleName();

    */
/**
     * Directory for raw data.
     *//*

    private static String raw_directory = "";

    */
/**
     * Android context.
     *//*

    private Context context;

    */
/**
     * List of restricted or ignored <code>DataSource</code>s.
     *//*

    private List<DataSource> restricted;

    */
/**
     * Gson
     *//*

    private Gson gson = new GsonBuilder().serializeNulls().create();

    */
/**
     * Network to use for high frequency uploads.
     *//*

    private String network_high_freq;

    */
/**
     * Network to use for low frequency uploads.
     *//*

    private String network_low_freq;

    */
/**
     * Subscription for observing file pruning.
     *//*

    private Subscription subsPrune;

    */
/**
     * Boolean trigger used to determine if the DataDescriptors are sufficient for the datastream
     **//*

    private boolean canUpload = true;


    */
/**
     * Constructor
     *
     * <p>
     * Sets up the uploader and introduces a list of data sources to not be uploaded.
     * </p>
     *
     * @throws IOException
     *//*

    public CerebralCortexWrapper(Context context, List<DataSource> restricted) throws IOException {
        Configuration configuration = ConfigurationManager.getInstance(context).configuration;
        this.context = context;
        this.restricted = restricted;
        this.network_high_freq = configuration.upload.network_high_frequency;
        this.network_low_freq = configuration.upload.network_low_frequency;

        raw_directory = FileManager.getDirectory(context, FileManager.INTERNAL_SDCARD_PREFERRED)
                + org.md2k.datakit.Constants.RAW_DIRECTORY;
    }

    */
/**
     * Sends broadcast messages containing the given message and an extra name, <code>"CC_Upload"</code>.
     *
     * @param message Message to put into the broadcast.
     *//*

    private void messenger(String message) {
        Intent intent = new Intent(Constants.CEREBRAL_CORTEX_STATUS);
        Time t = new Time(System.currentTimeMillis());
        String msg = t.toString() + ": " + message;
        intent.putExtra("CC_Upload", msg);
        Log.d("CerebralCortexMessenger", msg);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    */
/**
     * Main upload method for an individual <code>DataStream</code>.
     *
     * <p>
     * This method is responsible for offloading all unsynced data from low-frequency sources.
     * The data is offloaded to an SQLite database.
     * </p>
     *
     * @param dsc           <code>DataSourceClient</code> to upload.
     * @param ccWebAPICalls <code>CerebralCortex</code> Web API Calls.
     * @param ar            Authorization response.
     * @param dsMetadata    Metadata for the data stream.
     * @param dbLogger      Database logger
     *//*

    private void publishDataStream(DataSourceClient dsc, CCWebAPICalls ccWebAPICalls, AuthResponse ar,
                                   RegisterResponse regResponse ,DataStream dsMetadata, DatabaseLogger dbLogger) {
        Log.d("abc", "upload start...  id=" + dsc.getDs_id() + " source=" + dsc.getDataSource().getType());
        boolean cont = true;
        int BLOCK_SIZE_LIMIT = Constants.DATA_BLOCK_SIZE_LIMIT;
        long count = 0;
        canUpload = true;
        while (cont) {
            cont = false;
//Computed Data Store
            List<RowObject> objects;

            objects = dbLogger.queryLastKey(dsc.getDs_id(), Constants.DATA_BLOCK_SIZE_LIMIT);
            count = dbLogger.queryCount(dsc.getDs_id(), true).getSample();

            if (objects.size() > 0) {
                ArrayList<String> headers = generateHeaders(dsMetadata, dsc);
                int datalength = determineDataLength(objects, headers, dsMetadata, dsc);

                String outputTempFile = FileManager.getDirectory(context, FileManager.INTERNAL_SDCARD_PREFERRED) +
                        dsc.getDs_id() + "-" + randomUUID().toString() + ".msgpack";
                File outputfile = new File(outputTempFile);

                if (canUpload) {
                    try {
                        MessagePacker packer = MessagePack.newDefaultPacker(new FileOutputStream(outputfile));

                        // Pack headers
                        packer.packArrayHeader(headers.size());
                        for (String header : headers) {
                            packer.packString(header);
                        }

                        for (RowObject row : objects) {  // checks if datatype is an array
                            // Pack data
                            packer.packArrayHeader(datalength + 2);
                            packer.packLong(row.data.getDateTime() * 1000);
                            packer.packLong((row.data.getDateTime() + row.data.getOffset()) * 1000);
                            packData(packer, row.data);
                        }
                        packer.close();
                        File zippedmsgpack = msgpackZipper(outputfile);

                        messenger("Offloading data: " + dsc.getDs_id() + "(Remaining: " + count + ")");
                        Boolean resultUpload = ccWebAPICalls.putDataStream(regResponse.getHashId(), zippedmsgpack.getPath(), ar.getAccessToken());
                        Log.d(TAG, "result of upload " + resultUpload);
                        if (resultUpload) {
                            dbLogger.setSyncedBit(dsc.getDs_id(), objects.get(objects.size() - 1).rowKey);

                        } else {
                            Log.e(TAG, "Error uploading file: " + outputTempFile + " for SQLite database dump");
                            return;
                        }
                        // delete the temporary file here
                        zippedmsgpack.delete();

                    } catch (IOException e) {
                        Log.e("CerebralCortex", "MessagePack creation failed" + e);
                        e.printStackTrace();
                        return;
                    }


                } else {
                    Log.e(TAG, "DataDescriptor not properly defined. This datastream (" + dsMetadata.getName() + ") will not be uploaded. Ds_id: " + dsc.getDs_id());
                    break;
                }
                if (objects.size() == BLOCK_SIZE_LIMIT) {
                    cont = true;
                }
            }
        }
        Log.d(TAG, "upload done... prune...  id=" + dsc.getDs_id() + " source=" + dsc.getDataSource().getType());
    }

    */
/**
     * Generates a checksum for the given file.
     * @param uploadFile File to generate a checksum for.
     * @return The generated checksum.
     *//*

    private UploadMetadata checksumGen(File uploadFile) {
        MessageDigest md;
        UploadMetadata upMetadata;
        try {
            byte[] byteStream = new byte[(int) uploadFile.length()];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(uploadFile));
            buf.read(byteStream, 0, byteStream.length);
            buf.close();

            md = MessageDigest.getInstance("SHA-256");
            md.update(byteStream);
            byte[] digest = md.digest();

            upMetadata = new UploadMetadata(String.format("%064x", new java.math.BigInteger(1, digest)));
            return upMetadata;
        } catch (NoSuchAlgorithmException | IOException ignored) {
        }
        upMetadata = new UploadMetadata();
        return upMetadata;
    }

    */
/**
     * Constructs an ArrayList of headers from the name field of the <code>DataDescriptor</code>.
     *
     * @param dsMetadata Metadata of the datastream.
     * @param dsc        <code>DataSourceClient</code> used to get the <code>Ds_id</code> for troubleshooting.
     * @return The ArrayList of headers.
     *//*

    private ArrayList<String> generateHeaders(DataStream dsMetadata, DataSourceClient dsc) {

        int k = 0;
        for (String header : headers) {
            if (header.contains(" ")) {
                Log.e("MessagePack", "Removing spaces...");
                headers.set(k, header.replaceAll(" ", "_"));
            }
            k++;
        }
        return headers;
    }

    */
/**
     * Determines if the <code>DataDescriptor</code> properly describes each column of data. If the
     * <code>DataDescriptor</code> and number of data columns aren't the same, the datastream will not be uploaded.
     *
     * @param objects    List of <code>RowObjects</code> queried from the database.
     * @param headers    ArrayList of headers as determined from the <code>DataDescriptor</code>.
     * @param dsMetadata Metadata of the datastream.
     * @param dsc        <code>DataSourceClient</code> used to get the <code>Ds_id</code> for troubleshooting.
     * @return The number of columns of data.
     *//*

    private int determineDataLength(List<RowObject> objects, ArrayList<String> headers, DataStream dsMetadata, DataSourceClient dsc) {
        int datalength = 1;
        if (objects.get(0).data instanceof DataTypeBooleanArray) {
            datalength = ((DataTypeBooleanArray) objects.get(0).data).getSample().length;
        } else if (objects.get(0).data instanceof DataTypeDoubleArray) {
            datalength = ((DataTypeDoubleArray) objects.get(0).data).getSample().length;
        } else if (objects.get(0).data instanceof DataTypeFloatArray) {
            datalength = ((DataTypeFloatArray) objects.get(0).data).getSample().length;
        } else if (objects.get(0).data instanceof DataTypeIntArray) {
            datalength = ((DataTypeIntArray) objects.get(0).data).getSample().length;
        } else if (objects.get(0).data instanceof DataTypeJSONObject) {
            headers.set(2, headers.get(2) + "_json");
        } else if (objects.get(0).data instanceof DataTypeJSONObjectArray) {
            datalength = ((DataTypeJSONObjectArray) objects.get(0).data).getSample().size();
            headers.set(2, headers.get(2) + "_json");
        } else if (objects.get(0).data instanceof DataTypeLongArray) {
            datalength = ((DataTypeLongArray) objects.get(0).data).getSample().length;
        } else if (objects.get(0).data instanceof DataTypeStringArray) {
            datalength = ((DataTypeStringArray) objects.get(0).data).getSample().length;
        }
        if (datalength != headers.size() - 2) { // -1 because of "Timestamp"
            FL.e(TAG, "DataDescriptor not properly defined. This datastream (" + dsMetadata.getName() + ") will not be uploaded. Ds_id: " + dsc.getDs_id());
            Log.e(TAG, "DataDescriptor not properly defined. This datastream (" + dsMetadata.getName() + ") will not be uploaded. Ds_id: " + dsc.getDs_id());
            canUpload = false;
        }
        return datalength;
    }

    */
/**
     * Determines the specific <code>DataType</code> of the data and packs it accordingly.
     * Note that Array headers are not packed here as the data's array length is determined in <code>determingDataLength</code>
     * and packed in <code>publishDataStream</code>.
     *
     * @param packer MessagePacker that packs the data into the messagepack buffer.
     * @param data   Data to pack.
     * @return The amended MessagePacker.
     *//*

    private MessagePacker packData(MessagePacker packer, DataType data) {
        try {
            if (data instanceof DataTypeBoolean)
                packer.packBoolean(((DataTypeBoolean) data).getSample());
            else if (data instanceof DataTypeBooleanArray) {
                for (boolean datapoint : ((DataTypeBooleanArray) data).getSample())
                    packer.packBoolean(datapoint);
            } else if (data instanceof DataTypeByte)
                packer.packByte(((DataTypeByte) data).getSample());
            else if (data instanceof DataTypeByteArray) {
                for (byte datapoint : ((DataTypeByteArray) data).getSample())
                    packer.packByte(datapoint);
            } else if (data instanceof DataTypeDouble)
                packer.packDouble(((DataTypeDouble) data).getSample());
            else if (data instanceof DataTypeDoubleArray) {
                for (double datapoint : ((DataTypeDoubleArray) data).getSample())
                    packer.packDouble(datapoint);
            } else if (data instanceof DataTypeFloat)
                packer.packFloat(((DataTypeFloat) data).getSample());
            else if (data instanceof DataTypeFloatArray) {
                for (float datapoint : ((DataTypeFloatArray) data).getSample())
                    packer.packFloat(datapoint);
            } else if (data instanceof DataTypeInt)
                packer.packInt(((DataTypeInt) data).getSample());
            else if (data instanceof DataTypeIntArray) {
                for (int datapoint : ((DataTypeIntArray) data).getSample())
                    packer.packInt(datapoint);
            } else if (data instanceof DataTypeJSONObject) {
                // Get the JsonObject as a string and pack it as such. The header should be
                // modified so the cloud knows to decode this as a JsonObject.
                String jsonString = ((DataTypeJSONObject) data).getSample().toString();
                packer.packString(jsonString);
            } else if (data instanceof DataTypeJSONObjectArray) {
                JsonArray jsonarray = ((DataTypeJSONObjectArray) data).getSample();
                for (JsonElement datapoint : ((DataTypeJSONObjectArray) data).getSample()) {
                    // This code makes the assumption that all elements of the JsonArray are JsonObjects.
                    String jsonstring = datapoint.getAsJsonObject().toString();
                    packer.packString(jsonstring);
                }
            } else if (data instanceof DataTypeLong)
                packer.packLong(((DataTypeLong) data).getSample());
            else if (data instanceof DataTypeLongArray) {
                for (long datapoint : ((DataTypeLongArray) data).getSample())
                    packer.packLong(datapoint);
            } else if (data instanceof DataTypeString) {
                try {
                    packer.packString(((DataTypeString) data).getSample());
                } catch (Exception e) {
                    Log.e("MessagePack", "Null variable: " + data.toString());
                    packer.packString("NULL");
                }
            } else if (data instanceof DataTypeStringArray) {
                for (String datapoint : ((DataTypeStringArray) data).getSample())
                    packer.packString(datapoint);
            }
        } catch (IOException e) {
            Log.e("MessagePack", "Data packing failed " + e);
            e.printStackTrace();
            return packer;
        }
        return packer;
    }

    */
/**
     * Compresses the given MessagePack file using GZIP. The given MessagePack is deleted after compression.
     *
     * @param msgpack MessagePack to compress.
     *//*

    private File msgpackZipper(File msgpack) {
        try {
            Log.e("MessagePack", "Opening gzip buffer");
            String gzipfilename = msgpack.getAbsolutePath() + ".gz";
            File gzipfile = new File(gzipfilename);
            FileInputStream input = new FileInputStream(msgpack);
            GZIPOutputStream gzipout = new GZIPOutputStream(new FileOutputStream(gzipfile));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) != -1) {
                Log.e("MessagePack", "Writing buffer");
                gzipout.write(buffer, 0, len);
            }
            Log.e("MessagePack", "Closing files");
            gzipout.close();
            input.close();
            msgpack.delete();
            return gzipfile;
        } catch (IOException e) {
            Log.e("CerebralCortex", "Compressed file creation failed" + e);
            e.printStackTrace();
            return msgpack;
        }
    }

    */
/**
     * Frees space on the device by removing any raw data files that have already been synced to the cloud.
     *
     * @param prunes ArrayList of data source identifiers to delete.
     *//*

    private void deleteArchiveFile(final ArrayList<Integer> prunes) {
        final int[] current = new int[1];
        if (prunes == null || prunes.size() == 0)
            return;
        current[0] = 0;
        if (subsPrune != null && !subsPrune.isUnsubscribed())
            subsPrune.unsubscribe();

        subsPrune = Observable.range(1, 1000000).takeUntil(new Func1<Integer, Boolean>() {
            */
/**
             * Deletes the files in the directory when called.
             *
             * @param aLong Needed for proper override.
             * @return Whether the deletion was completed or not.
             *//*

            @Override
            public Boolean call(Integer aLong) {
                Log.d("abc", "current=" + current[0] + " size=" + prunes.size());
                if (current[0] >= prunes.size())
                    return true;
                File directory = new File(raw_directory + "/raw" + current[0]);
                FilenameFilter ff = new FilenameFilter() {
                    */
/**
                     * Method checks if the file is marked archive or corrupt.
                     *
                     * @param dir Directory the file is in.
                     * @param filename File to check.
                     * @return Whether the file is acceptable or not.
                     *//*

                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.contains("_archive") || filename.contains("_corrupt"))
                            return true;
                        return false;
                    }
                };
                File[] files = directory.listFiles(ff);
                for (int i = 0; files != null && i < files.length; i++) {
                    files[i].delete();
                }
                current[0]++;
                return false;
            }
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer aLong) {
            }
        });
    }

    */
/**
     * Main upload method for an individual raw <code>DataStream</code>.
     *
     * <p>
     * This method is responsible for offloading all unsynced data from high-frequency sources.
     * </p>
     *
     * @param dsc           <code>DataSourceClient</code>
     * @param ccWebAPICalls
     * @param ar
     * @param dsMetadata    Metadata for the given data stream.
     *//*

    private void publishDataFiles(DataSourceClient dsc, CCWebAPICalls ccWebAPICalls, AuthResponse ar,
                                  RegisterResponse regResponse, DataStream dsMetadata) {
        canUpload = true;
        Log.d("HFUpload", "Starting HFupload");
        File directory = new File(raw_directory + "/raw" + dsc.getDs_id());
        FilenameFilter ff = new FilenameFilter() {
            */
/**
             * Method checks if the file is marked archive or corrupt and if so, rejects them.
             *
             * @param dir Directory the file is in.
             * @param filename File to check.
             * @return Whether the file is acceptable or not.
             *//*

            @Override
            public boolean accept(File dir, String filename) {
                if (filename.contains("_archive") || filename.contains("_corrupt") || filename.contains(".gz") || filename.contains(".msgpack"))
                    return false;
                return true;
            }
        };

        File[] files = directory.listFiles(ff);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");
        File zippedOutput;

        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                Long fileTimestamp = Long.valueOf(file.getName().substring(0, 10));
                Long currentTimestamp = Long.valueOf(dateFormat.format(new Date()));

                if (fileTimestamp < currentTimestamp) {
                    Log.d(TAG, file.getAbsolutePath());

                    File outputfile = new File(file.getAbsolutePath() + ".msgpack");
                    Log.d("HFUpload", "Generating headers");
                    ArrayList<String> headers = generateHeaders(dsMetadata, dsc);

                    try {
                        BufferedReader lineReader = new BufferedReader(new FileReader(file));
                        String line = lineReader.readLine();
                        String[] firstLineArray = line.split(",");
                        if (firstLineArray.length != headers.size()) {
                            canUpload = false;
                        }

                        if (canUpload) {
                            MessagePacker rawPacker = MessagePack.newDefaultPacker(new FileOutputStream(outputfile));
                            rawPacker.packArrayHeader(headers.size());
                            for (String header: headers)
                                rawPacker.packString(header);
                            while ((line = lineReader.readLine()) != null) {
                                String[] lineArray = line.split(",");
                                if (lineArray.length != headers.size()) {
                                    Log.e(TAG, "Line in raw data file missing or corrupt. Line skipped.");
                                } else {
                                    rawPacker.packArrayHeader(lineArray.length);
                                    int i = 0;
                                    try {
                                        for (String datapoint : lineArray) {
                                            if (i < 2) {
                                                if (i == 0) {
                                                    rawPacker.packLong(Long.parseLong(datapoint) * 1000);
                                                } else {
                                                    rawPacker.packLong(Long.parseLong(datapoint) + Long.parseLong(lineArray[i - 1]) * 1000);
                                                }
                                            } else
                                                rawPacker.packDouble(Double.parseDouble(datapoint));
                                            i++;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Line in raw data file missing or corrupt. Line skipped.");
                                        Log.e(TAG, e.getMessage() + " " + e.getCause());
                                    }
                                }
                            }
                            rawPacker.close();
                            zippedOutput = msgpackZipper(outputfile);
                        } else {
                            Log.e(TAG, "DataDescriptor not properly defined. This datastream (" + dsMetadata.getName() + ") will not be uploaded. Ds_id: " + dsc.getDs_id());
                            String headerstring = "";
                            for (String header: headers) {
                                headerstring += header;
                            }
                            Log.e(TAG, "Headers: " + headerstring);

                            break;
                        }

                    } catch (IOException e) {
                        Log.e("CerebralCortex", "Raw Messagepack creation failed " + e);
                        e.printStackTrace();
                        return;
                    }
                    UploadMetadata upMetadata = checksumGen(file);
                    Boolean resultUpload = ccWebAPICalls.putDataStream(regResponse.getHashId(), zippedOutput.getAbsolutePath(), ar.getAccessToken());
                    zippedOutput.delete();
                    if (resultUpload) {
                        File newFile = new File(file.getAbsolutePath());
                        newFile.delete();
                    } else {
                        Log.e(TAG, "Error uploading file: " + file.getName());
                        return;
                    }
                }
            }
        }
    }


    */
/**
     * Checks if the given data source is in the restricted list.
     *
     * @param dsc <code>DataSourceClient</code> to search for.
     * @return Whether the given data source is the restricted list.
     *//*

    private boolean inRestrictedList(DataSourceClient dsc) {
        for (DataSource d : restricted) {
            if (dsc.getDataSource().getType().equals(d.getType())) {
                return true;
            }
        }
        return false;
    }

    */
/**
     * Executes the upload routine.
     *
     * <p>
     * The upload routine is as follows:
     * <ul>
     * <li>First, the user is authenticated.</li>
     * <li>Then, for each data source:</li>
     * <ul>
     * <li>data source is checked for restriction.</li>
     * <li>low frequency network connection type is checked for validity.</li>
     * <li>low frequency data is published to the server.</li>
     * <li>high frequency network connection type is checked for validity.</li>
     * <li>high frequency data is published to the server.</li>
     * </ul>
     * <li>After all data sources have been published, the synced data is removed from the database.</li>
     * <li>And finally, the raw files are deleted.</li>
     * </ul>
     * </p>
     *//*

    public void run() {

        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        List<DataSourceClient> dataSourceClients = dbLogger.find(dataSourceBuilder.build());
        ArrayList<Integer> prune = new ArrayList<>();
        ArrayList<Integer> pruneFiles = new ArrayList<>();

        // Iterate over the data sources
        for (DataSourceClient dsc : dataSourceClients) {

            // Check if the current data source is on the restricted list.
            if (!inRestrictedList(dsc)) {
                MetadataBuilder metadataBuilder = new MetadataBuilder();
                DataStream dsMetadata = metadataBuilder.buildDataStreamMetadata(ar.getUserUuid(), dsc);
                StreamMetadata streamMetadata = metadataBuilder.buildStreamMetadata(ar.getUserUuid(), dsc);
                RegisterResponse regResp = ccWebAPICalls.registerDataStream(ar.getAccessToken(), streamMetadata);

                // Check for valid low frequency network connection type.
                if (isNetworkConnectionValid(network_low_freq)) {
                    Log.d("abc", "trying to upload from database id=" + dsc.getDs_id());
                    messenger("Publishing data for " + dsc.getDs_id() + " (" + dsc.getDataSource().getId() + ":" + dsc.getDataSource().getType() + ") to " + dsMetadata.getIdentifier());

                    // Publish the data to the server.
                    publishDataStream(dsc, ccWebAPICalls, ar, regResp, dsMetadata, dbLogger);
                    prune.add(dsc.getDs_id());
                }

                // Check for valid high frequency network connection type.
                if (isNetworkConnectionValid(network_high_freq)) {
                    Log.d("abc", "trying to upload from file id=" + dsc.getDs_id());
                    messenger("Publishing raw data for " + dsc.getDs_id() + " (" + dsc.getDataSource().getId() + ":" + dsc.getDataSource().getType() + ") to " + dsMetadata.getIdentifier());
                    pruneFiles.add(dsc.getDs_id());

                    // Publish the data to the server.
                    publishDataFiles(dsc, ccWebAPICalls, ar, regResp, dsMetadata);
                }
            }
        }

        // Remove SQLite data that has been synced.
        dbLogger.pruneSyncData(prune);

        // Delete raw archive files that have been synced.
        deleteArchiveFile(pruneFiles);
        messenger("Upload Complete");
    }
*/
}