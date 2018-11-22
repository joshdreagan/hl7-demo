/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.apache.camel.examples;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

  private final IngestProperties ingest = new IngestProperties();
  private final DataCorrectionProcessProperties dataCorrection = new DataCorrectionProcessProperties();
  private final NotificationProperties notification = new NotificationProperties();

  public IngestProperties getIngest() {
    return ingest;
  }

  public DataCorrectionProcessProperties getDataCorrection() {
    return dataCorrection;
  }

  public NotificationProperties getNotification() {
    return notification;
  }

  public static class IngestProperties {
    
    private final FileIngestProperties file = new FileIngestProperties();
    private final MllpIngestProperties mllp = new MllpIngestProperties();

    public FileIngestProperties getFile() {
      return file;
    }

    public MllpIngestProperties getMllp() {
      return mllp;
    }

    public static class FileIngestProperties {
      
      private String directory;

      public String getDirectory() {
        return directory;
      }

      public void setDirectory(String directory) {
        this.directory = directory;
      }
    }

    public static class MllpIngestProperties {
      
      private String port;

      public String getPort() {
        return port;
      }

      public void setPort(String port) {
        this.port = port;
      }
    }
  }
  
  public static class DataCorrectionProcessProperties {
    
    private String prefix = "http";
    private String host = "localhost";
    private String port = "8080";
    private String path;
    private String username;
    private String password;
    private String callbackUrl;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getPort() {
      return port;
    }

    public void setPort(String port) {
      this.port = port;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getCallbackUrl() {
      return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
    }
  }
  
  public static class NotificationProperties {
    
    private String prefix = "smtp";
    private String host = "localhost";
    private String port = "2525";
    private String username = "anonymous";
    private String password;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getPort() {
      return port;
    }

    public void setPort(String port) {
      this.port = port;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
