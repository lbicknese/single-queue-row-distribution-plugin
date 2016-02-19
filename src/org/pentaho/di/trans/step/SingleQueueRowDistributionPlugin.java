/*! ******************************************************************************
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

package org.pentaho.di.trans.step;

import java.util.concurrent.TimeUnit;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.gui.PrimitiveGCInterface;
import org.pentaho.di.core.gui.PrimitiveGCInterface.EImage;
import org.pentaho.di.core.row.RowMetaInterface;

/**
 * This plug-in was altered from the sample Overflow Row Distribution plug-in described in the Pentaho Wiki
 * http://wiki.pentaho.com/display/EAI/PDI+Row+Distribution+Plugin+Development.
 * 
 * @author Luke Bicknese
 *
 */
@RowDistributionPlugin(code = "SingleQueue", name = "Single Queue", description = "Mimics concurrent processing from a single queue.")
public class SingleQueueRowDistributionPlugin implements RowDistributionInterface {

	/**
	 * Returns the code for the plug-in.
	 * 
	 * @return String
	 */
	@Override
	public String getCode() {
		return "SingleQueue";
	}

	/**
	 * Returns he description for the plug-in.
	 * 
	 * @return String
	 */
	@Override
	public String getDescription() {
		return "Mimics concurrent processing from a single queue.";
	}

	/**
	 * Distributes a row from the Input step to an output Step.
	 * 
	 * @param rowMeta	the RowMetaInterface meta data for the row data to be distributed.
	 * @param row		the row data to be distributed.
	 * @param step		the StepInterface step reference for the input step that is distributing its data.
	 */
	@Override
	public void distributeRow(RowMetaInterface rowMeta, Object[] row, StepInterface step) throws KettleStepException {

		// Get current outputRowSet
		RowSet rowSet = step.getOutputRowSets().get(step.getCurrentOutputRowSetNr());
		
		// Initialize added Boolean
		boolean added = false;
		
		/*
		 * Differently than the example Overflow distribution, check that the step
		 * has not been stopped. Before it was looping until added was true, which
		 * didn't really do anything because as soon as added is true there is a 
		 * break from the loop.
		 */
		while (!step.isStopped()) {

			/*
			 * Differently than the example Overflow distribution, this implementation forces
			 * what works as a single work queue. The row will be placed on a queue, and then
			 * subsequently polled. If we are able to poll the row off of the queue, then try
			 * the next queue. This strategy should ensure that work is only ever assigned to
			 * a worker that is ready to work on it.
			 */
			added = rowSet.putRowWait(rowMeta, row, 1, TimeUnit.NANOSECONDS);
			
			/*
			 * Wait a little bit before attempting to poll the row off the queue to give the
			 * worker thread time to poll the row.
			 */
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			/*
			 * Poll the row off of the queue.
			 */
			Object[] putRow = rowSet.getRowWait(1, TimeUnit.NANOSECONDS);
			
			/*
			 * If we were able to poll the row off of the queue, then it was not added.
			 */
			if (putRow != null) {
				added = false;
			}
			
			/*
			 * Differently than the example Overflow distribution, always increment to the
			 * next output RowSet. This provides more of a round-robin approach, but will
			 * put a row in the next available output RowSet.
			 */
			step.setCurrentOutputRowSetNr(step.getCurrentOutputRowSetNr() + 1);
			if (step.getCurrentOutputRowSetNr() > step.getOutputRowSets().size() - 1) {
				step.setCurrentOutputRowSetNr(0);
			}

			/*
			 * If the row was added to a RowSet, break from the while loop, we'ere done.
			 */
			if (added) {
				break;
			}
			
			/*
			 * If the row was not added to the RowSet, get the next RowSet and try again.
			 */
			rowSet = step.getOutputRowSets().get(step.getCurrentOutputRowSetNr());
			
		}
		
	}

	/**
	 * Returns an image to represent the distribution method.
	 * 
	 * @return	EImage
	 */
	@Override
	public EImage getDistributionImage() {
		return PrimitiveGCInterface.EImage.LOAD_BALANCE;
	}

}
