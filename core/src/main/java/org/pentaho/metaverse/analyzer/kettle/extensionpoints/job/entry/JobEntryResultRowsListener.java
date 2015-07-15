/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.metaverse.analyzer.kettle.extensionpoints.job.entry;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.job.JobExecutionExtension;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.dictionary.DictionaryConst;
import org.pentaho.metaverse.analyzer.kettle.extensionpoints.BaseRuntimeExtensionPoint;
import org.pentaho.metaverse.analyzer.kettle.extensionpoints.job.JobRuntimeExtensionPoint;
import org.pentaho.metaverse.api.IAnalyzer;
import org.pentaho.metaverse.api.IComponentDescriptor;
import org.pentaho.metaverse.api.IDocument;
import org.pentaho.metaverse.api.IMetaverseBuilder;
import org.pentaho.metaverse.api.IMetaverseNode;
import org.pentaho.metaverse.api.INamespace;
import org.pentaho.metaverse.api.MetaverseComponentDescriptor;
import org.pentaho.metaverse.api.Namespace;
import org.pentaho.metaverse.api.analyzer.kettle.BaseKettleMetaverseComponent;
import org.pentaho.metaverse.util.MetaverseUtil;

import java.util.List;

@ExtensionPoint(
  description = "Job entry result rows listener",
  extensionPointId = "JobAfterJobEntryExecution",
  id = "jobEntryResultRows" )
public class JobEntryResultRowsListener extends BaseKettleMetaverseComponent implements ExtensionPointInterface {

  /**
   * This method is called by the Kettle code when a job entry is about to start
   *
   * @param log    the logging channel to log debugging information to
   * @param object The subject object that is passed to the plugin code
   * @throws org.pentaho.di.core.exception.KettleException In case the plugin decides that an error has occurred
   *                                                       and the parent process should stop.
   */
  @Override
  public void callExtensionPoint( LogChannelInterface log, Object object ) throws KettleException {
    JobExecutionExtension jobExec = (JobExecutionExtension) object;
    JobEntryCopy jobEntryCopy = jobExec.jobEntryCopy;
    if ( jobEntryCopy != null ) {
      JobEntryInterface meta = jobEntryCopy.getEntry();
      if ( meta != null ) {
        // Create a document for the Job
        final String clientName = BaseRuntimeExtensionPoint.getExecutionEngineInfo().getName();
        final INamespace namespace = new Namespace( clientName );
        final IMetaverseBuilder builder = MetaverseUtil.getDocumentController().getMetaverseBuilder();
        final IMetaverseNode designNode = builder.getMetaverseObjectFactory()
          .createNodeObject( clientName, clientName, DictionaryConst.NODE_TYPE_LOCATOR );
        builder.addNode( designNode );

        JobMeta jobMeta = jobEntryCopy.getParentJobMeta();
        String id = JobRuntimeExtensionPoint.getFilename( jobMeta );

        IDocument metaverseDocument = MetaverseUtil.createDocument( namespace, jobMeta )


        // Create a node for this job entry
        IComponentDescriptor ds = new MetaverseComponentDescriptor(
          jobEntryCopy.getName(),
          DictionaryConst.NODE_TYPE_JOB,
          descriptor.getNamespace().getParentNamespace() );

        IMetaverseNode transformationNode = createNodeFromDescriptor( ds );
        transformationNode.setProperty( DictionaryConst.PROPERTY_NAMESPACE, ds.getNamespaceId() );
        transformationNode.setProperty( DictionaryConst.PROPERTY_PATH, transPath );
        transformationNode.setLogicalIdGenerator( DictionaryConst.LOGICAL_ID_GENERATOR_DOCUMENT );

        // Create a new descriptor for the RowsFromResult step.
        IComponentDescriptor stepDescriptor = new MetaverseComponentDescriptor( IAnalyzer.NONE,
          DictionaryConst.NODE_TYPE_TRANS_STEP, subTransNode, descriptor.getContext() );

        // Create a new node for the step, to be used as the parent of the the field we want to link to
        IMetaverseNode subTransStepNode = createNodeFromDescriptor( stepDescriptor );
        List<RowMetaAndData> rows = jobExec.result.getRows();
        if ( !Const.isEmpty( rows ) ) {
          // Only need the first one, the metadata needs to be the same for every object
          RowMetaAndData row = rows.get( 0 );
          if ( row != null ) {
            RowMetaInterface rowMeta = row.getRowMeta();
            if ( rowMeta != null ) {
              // Create nodes for the result row fields
              for ( int i = 0; i < rowMeta.size(); i++ ) {
                ValueMetaInterface field = rowMeta.getValueMeta( i );
                if ( field != null ) {
                  IComponentDescriptor stepFieldDescriptor = new MetaverseComponentDescriptor( field,
                    DictionaryConst.NODE_TYPE_TRANS_FIELD, subTransStepNode, descriptor.getContext() );
                }
              }
            }
          }
        }
      }
    }
  }
}
