/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.classify.*;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

import javax.swing.*;

import gnu.trove.TObjectDoubleIterator;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author Edoardo Airoldi
 * Date: Mar 15, 2004
 */

public class MultinomialClassifier implements Classifier, Visible,Serializable
{

  static private Logger log = Logger.getLogger(MultinomialClassifier.class);

  private ArrayList classNames;
  private ArrayList classParameters;
  private ArrayList featureGivenClassParameters;

  // constructor
  public MultinomialClassifier()
  {
    this.classNames = new ArrayList();
    this.classParameters = new ArrayList();
    this.featureGivenClassParameters = new ArrayList();
    this.featureGivenClassParameters.add( new WeightedSet() );
  }


  //
  // methods in Classifier interface
  //
  public ClassLabel classification(Instance instance)
  {
    double[] score = score(instance);
    int maxIndex = 0;
    for (int i=0; i<score.length; i++)
    {
      if ( score[i]>score[maxIndex] )
      {
        maxIndex=i;
      }
    }
    //System.out.println( classNames.get(0)+","+score[0]+" "+classNames.get(1)+","+score[1]);
    return new ClassLabel( (String)classNames.get(maxIndex) );
  }

  public double[] score(Instance instance)
  {
    //System.out.println( "class="+classNames.get(0)+" counts="+featureGivenClassParameters.get(0) );
    //System.out.println( "class="+classNames.get(1)+" counts="+featureGivenClassParameters.get(1) );
    double[] score = new double[classNames.size()];
    for (int i=0; i<classNames.size(); i++)
    {
      score[i] = 0.0;
      //System.out.println("instance="+instance);
      for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
        Feature f = j.nextFeature();
        double featureCounts = instance.getWeight(f);
        double featureProb = ((WeightedSet)featureGivenClassParameters.get(i)).getWeight(f);
        double classProb = ((Double)classParameters.get(i)).doubleValue();
        //System.out.println("feature="+f+" counts="+featureCounts+" prob="+featureProb+" class="+classProb);
        score[i] += featureCounts*Math.log(featureProb)+Math.log(classProb);
      }
    }
    return score;
  }

  public String explain(Instance instance)
  {
    StringBuffer buf = new StringBuffer("");
    for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
      Feature f = j.nextFeature();
      if (buf.length()>0) buf.append("\n + ");
      else buf.append("   ");
      //buf.append( f+"<"+instance.getWeight(f)+"*"+featureScore(f)+">");
    }
    //buf.append( "\n + bias<"+featureScore( BIAS_TERM )+">" );
    buf.append("\n = "+score(instance) );
    return buf.toString();
  }


  //
  // Get, Set, Check
  //
  public void reset()
  {
    this.classParameters = new ArrayList();
    this.featureGivenClassParameters = new ArrayList();
    //this.featureGivenClassParameters.add( new WeightedSet() );
  }

  public boolean isPresent(ClassLabel label)
  {
    boolean isPresent = false;
    for (int i=0; i<classNames.size(); i++ )
    {
      if ( classNames.get(i).equals(label.bestClassName()) ) { isPresent= true; }
    }
    return isPresent;
  }

  public void addValidLabel(ClassLabel label)
  {
    classNames.add( label.bestClassName() );
  }

  public ClassLabel getLabel(int i)
  {
    return new ClassLabel( (String)classNames.get(i) );
  }

  public int indexOf(ClassLabel label)
  {
    return classNames.indexOf( label.bestClassName() );
  }

  public void setFeatureGivenClassParameter(Feature f, int j, double probabilityOfOccurrence)
  {
    WeightedSet wset;
    try
    {
      wset = (WeightedSet)featureGivenClassParameters.get(j);
      wset.add( f,probabilityOfOccurrence );
      featureGivenClassParameters.set( j,wset );
    }
    catch (Exception t)
    {
      wset = null;
      wset = new WeightedSet();
      wset.add( f,probabilityOfOccurrence ) ;
      featureGivenClassParameters.add( j,wset );
    }
  }

  public void setClassParameter(int j, double probabilityOfOccurrence)
  {
    classParameters.add( j,new Double(probabilityOfOccurrence) );
  }

  public Feature.Looper featureIterator()
  {
    // 1. create a new WeightedSet with all features
    TObjectDoubleHashMap map = new TObjectDoubleHashMap();
    for (int i=0; i<classNames.size(); i++)
    {
      WeightedSet wset = (WeightedSet)featureGivenClassParameters.get(i);
      for (Iterator j=wset.iterator(); j.hasNext();)
      {
        Feature f = (Feature)j.next();
        double w = wset.getWeight(f);
        map.put(f,w);
      }
    }
    // 2. create global feature iterator
    final TObjectDoubleIterator ti = map.iterator();
    Iterator i = new Iterator() {
        public boolean hasNext() { return ti.hasNext(); }
        public Object next() { ti.advance(); return ti.key(); }
        public void remove() { ti.remove(); }
      };
    return new Feature.Looper(i);
  }

  public Object[] keys()
  {
    TObjectDoubleHashMap map = new TObjectDoubleHashMap();
    for (int i=0; i<classNames.size(); i++)
    {
      WeightedSet wset = (WeightedSet)featureGivenClassParameters.get(i);
      for (Iterator j=wset.iterator(); j.hasNext();)
      {
        Feature f = (Feature)j.next();
        double w = wset.getWeight(f);
        map.put(f,w);
      }
    }
    return map.keys();
  }


  //
  // GUI related stuff
  //
  public Viewer toGUI()
  {
    Viewer gui = new ControlledViewer(new MyViewer(), new MultinomialClassifierControls());
    gui.setContent(this);
    return gui;
  }

  static private class MultinomialClassifierControls extends ViewerControls
  {
    // how to sort
    private JRadioButton absoluteValueButton,valueButton,nameButton,noneButton;
    public void initialize()
    {
      add(new JLabel("Sort by"));
      ButtonGroup group = new ButtonGroup();;
      nameButton = addButton("name",group,true);
      valueButton = addButton("weight",group,false);
      absoluteValueButton = addButton("|weight|",group,false);
      setFloatable(false);
    }
    private JRadioButton addButton(String s,ButtonGroup group,boolean selected)
    {
      JRadioButton button = new JRadioButton(s,selected);
      group.add(button);
      add(button);
      button.addActionListener(this);
      return button;
    }
  }

  static private class MyViewer extends ComponentViewer implements Controllable
  {
    private MultinomialClassifierControls controls = null;
    private MultinomialClassifier h = null;

    public void applyControls(ViewerControls controls)
    {
      this.controls = (MultinomialClassifierControls)controls;
      setContent(h,true);
      revalidate();
    }
    public boolean canReceive(Object o) {	return o instanceof MultinomialClassifier;	}

    public JComponent componentFor(Object o)
    {
      h = (MultinomialClassifier)o;
      Object[] keys = h.keys();
      Object[][] tableData = new Object[keys.length][(h.classNames.size()+1)];
      int k=0;
      for (Feature.Looper i=h.featureIterator(); i.hasNext(); ) {
        Feature f = i.nextFeature();
        tableData[k][0] = f;
        for (int l=0; l<h.classNames.size(); l++)
        {
          tableData[k][(l+1)] = new Double( ((WeightedSet)h.featureGivenClassParameters.get(l)).getWeight(f) );
;
        }
        k++;
      }
      if (controls!=null) {
        Arrays.sort(
          tableData,
          new Comparator() {
            public int compare(Object a,Object b) {
              Object[] ra = (Object[])a;
              Object[] rb = (Object[])b;
              if (controls.nameButton.isSelected())
                return ra[0].toString().compareTo(rb[0].toString());
              Double da = (Double)ra[1];
              Double db = (Double)rb[1];
              if (controls.valueButton.isSelected())
                return MathUtil.sign( db.doubleValue() - da.doubleValue() );
              else
                return MathUtil.sign( Math.abs(db.doubleValue()) - Math.abs(da.doubleValue()) );
            }
          });
      }
      String[] columnNames = new String[(h.classNames.size()+1)];
      columnNames[0] = "Feature Name";
      for (int i=0; i<h.classNames.size(); i++)
      {
        columnNames[(i+1)] = "Wgt C"+i;
      }
      JTable table = new JTable(tableData,columnNames);
      monitorSelections(table,0);
      return new JScrollPane(table);
    }
  }


  public String toString() { return null; }

}
