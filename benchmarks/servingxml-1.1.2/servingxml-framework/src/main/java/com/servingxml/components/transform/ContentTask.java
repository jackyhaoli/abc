/**
 *  ServingXML
 *  
 *  Copyright (C) 2006  Daniel Parker
 *    daniel.parker@servingxml.com 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/

package com.servingxml.components.transform;

import com.servingxml.components.task.Task;
import com.servingxml.app.xmlpipeline.XmlPipeline;
import com.servingxml.components.content.Content;

/**
 * Defines an interface for a component
 *
 * 
 * @author Daniel A. Parker (daniel.parker@servingxml.com)
 */

public interface ContentTask extends Task, Content { 
  public static ContentTask[] EMPTY_ARRAY = new ContentTask[0];
}
