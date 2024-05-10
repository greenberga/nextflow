/*
 * Copyright 2013-2024, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Task Execution Service
 *
 * OpenAPI spec version: 1.1.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package nextflow.ga4gh.tes.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import nextflow.ga4gh.tes.client.model.TesTask;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * ListTasksResponse describes a response from the ListTasks endpoint.
 */
@Schema(description = "ListTasksResponse describes a response from the ListTasks endpoint.")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2023-08-15T14:10:09.878Z[GMT]")

public class TesListTasksResponse {
  @SerializedName("tasks")
  private List<TesTask> tasks = new ArrayList<TesTask>();

  @SerializedName("next_page_token")
  private String nextPageToken = null;

  public TesListTasksResponse tasks(List<TesTask> tasks) {
    this.tasks = tasks;
    return this;
  }

  public TesListTasksResponse addTasksItem(TesTask tasksItem) {
    this.tasks.add(tasksItem);
    return this;
  }

   /**
   * List of tasks. These tasks will be based on the original submitted task document, but with other fields, such as the job state and logging info, added/changed as the job progresses.
   * @return tasks
  **/
  @Schema(required = true, description = "List of tasks. These tasks will be based on the original submitted task document, but with other fields, such as the job state and logging info, added/changed as the job progresses.")
  public List<TesTask> getTasks() {
    return tasks;
  }

  public void setTasks(List<TesTask> tasks) {
    this.tasks = tasks;
  }

  public TesListTasksResponse nextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
    return this;
  }

   /**
   * Token used to return the next page of results. This value can be used in the &#x60;page_token&#x60; field of the next ListTasks request.
   * @return nextPageToken
  **/
  @Schema(description = "Token used to return the next page of results. This value can be used in the `page_token` field of the next ListTasks request.")
  public String getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TesListTasksResponse tesListTasksResponse = (TesListTasksResponse) o;
    return Objects.equals(this.tasks, tesListTasksResponse.tasks) &&
        Objects.equals(this.nextPageToken, tesListTasksResponse.nextPageToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasks, nextPageToken);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TesListTasksResponse {\n");
    
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
    sb.append("    nextPageToken: ").append(toIndentedString(nextPageToken)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
