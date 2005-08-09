package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.multi.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Train a text classifier.
 *
 * @author William Cohen
 */

public class TrainMultiClassifier extends UIMain
{
    private static Logger log = Logger.getLogger(TrainClassifier.class);

    // private data needed to train a classifier

    private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
    private CommandLineUtil.MultiClassificationSignalParams signal = new CommandLineUtil.MultiClassificationSignalParams(base);
    private CommandLineUtil.TrainClassifierParams train = new CommandLineUtil.TrainClassifierParams();
    private MultiClassifier classifier = null;

    public CommandLineUtil.SaveParams getSaveParameters() { return save; }
    public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
    public CommandLineUtil.MultiClassificationSignalParams getSignalParameters() { return signal; } 
    public void setSignalParameters(CommandLineUtil.MultiClassificationSignalParams p) { signal=p; } 
    public CommandLineUtil.TrainClassifierParams getAdditionalParameters() { return train; } 
    public void setAdditionalParameters(CommandLineUtil.TrainClassifierParams p) { train=p; } 


    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,save,signal,train});
    }

    //
    // do the experiment
    // 

    public void doMain()
    {
	// check that inputs are valid
	if (train.learner==null) throw new IllegalArgumentException("-learner must be specified");
	if (signal.multiSpanProp==null) 
			throw new IllegalArgumentException("-multiSpanProp  must be specified");

	// construct the dataset
	MultiDataset d = CommandLineUtil.toMultiDataset(base.labels,train.fe,signal.multiSpanProp);
	if(signal.cross) d=d.annotateData();
	if (train.showData) {
	    System.out.println("Trying to show the Dataset");
	    new ViewerFrame("Dataset", d.toGUI());
	}

	// train the classifier
	classifier = new MultiDatasetClassifierTeacher(d).train(train.learner);

	if (base.showResult) {
	    Viewer cv = new SmartVanillaViewer();
	    cv.setContent(classifier);
	    new ViewerFrame("Classifier",cv); 
	}

	MultiClassifierAnnotator ann = new MultiClassifierAnnotator(train.fe,classifier,signal.multiSpanProp);

	if (save.saveAs!=null) {
	    try {
		IOUtil.saveSerialized((Serializable)ann,save.saveAs);
	    } catch (IOException e) {
		throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
	    }
	}
    }

    public Object getMainResult() { return classifier; }

    public static void main(String args[])
    {
	new TrainMultiClassifier().callMain(args);
    }
}