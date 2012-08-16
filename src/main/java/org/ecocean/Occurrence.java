package org.ecocean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;
import java.util.Arrays;

/**
 * Whereas an Encounter is meant to represent one MarkedIndividual at one point in time and space, an Occurrence
 * is meant to represent several Encounters that occur in a natural grouping (e.g., a pod of dolphins). Ultimately
 * the goal of the Encounter class is to represent associations among MarkedIndividuals that are commonly 
 * sighted together.
 * 
 * @author Jason Holmberg
 *
 */
public class Occurrence implements java.io.Serializable{
  
  
  
  /**
   * 
   */
  private static final long serialVersionUID = -7545783883959073726L;
  private ArrayList<Encounter> encounters;
  private String occurrenceID;
  private int individualCount;
  private String groupBehavior;
  //additional comments added by researchers
  private String comments = "None";
  private String modified;
  private String locationID;
  
  
  //empty constructor used by the JDO enhancer
  public Occurrence(){}
  
  /**
   * Class constructor.
   * 
   * 
   * @param occurrenceID A unique identifier for this occurrence that will become its primary key in the database.
   * @param enc The first encounter to add to this occurrence. 
   */
  public Occurrence(String occurrenceID, Encounter enc){
    this.occurrenceID=occurrenceID;
    encounters=new ArrayList<Encounter>();
    encounters.add(enc);
    
    if((enc.getLocationID()!=null)&&(!enc.getLocationID().equals("None"))){this.locationID=enc.getLocationID();}
    
  }
  
  public void addEncounter(Encounter enc){
    if(encounters==null){encounters=new ArrayList<Encounter>();}
    encounters.add(enc);
    
    if((enc.getLocationID()!=null)&&(!enc.getLocationID().equals("None"))){this.locationID=enc.getLocationID();}
    
    
  }
  
  public ArrayList<Encounter> getEncounters(){
    return encounters;
  }
  
  public void removeEncounter(Encounter enc){
    if(encounters!=null){
      encounters.remove(enc);
    }
  }
  
  public int getNumberEncounters(){
    if(encounters==null) {return 0;}
    else{return encounters.size();}
  }
  
  public void setEncounters(ArrayList<Encounter> encounters){this.encounters=encounters;}
  
  public ArrayList<String> getMarkedIndividualNamesForThisOccurrence(){
    ArrayList<String> names=new ArrayList<String>();
    try{
      int size=getNumberEncounters();
    
      for(int i=0;i<size;i++){
        Encounter enc=encounters.get(i);
        if((enc.getIndividualID()!=null)&&(!enc.getIndividualID().equals("Unassigned"))&&(!names.contains(enc.getIndividualID()))){names.add(enc.getIndividualID());}
      }
    }
    catch(Exception e){e.printStackTrace();}
    return names;
  }
  
  public String getOccurrenceID(){return occurrenceID;}
  

  public int getIndividualCount(){return individualCount;}
  public void setIndividualCount(int count){this.individualCount=count;}
  
  public String getGroupBehavior(){return groupBehavior;}
  public void setGroupBehavior(String behavior){this.groupBehavior=behavior;}

  public ArrayList<SinglePhotoVideo> getAllRelatedMedia(){
    int numEncounters=encounters.size();
    ArrayList<SinglePhotoVideo> returnList=new ArrayList<SinglePhotoVideo>();
    for(int i=0;i<numEncounters;i++){
     Encounter enc=encounters.get(i);
     if(enc.getSinglePhotoVideo()!=null){
       returnList.addAll(enc.getSinglePhotoVideo());
     }
    }
    return returnList;
  }
  
  //you can choose the order of the EncounterDateComparator
  public Encounter[] getDateSortedEncounters(boolean reverse) {
  Vector final_encs = new Vector();
  for (int c = 0; c < encounters.size(); c++) {
    Encounter temp = (Encounter) encounters.get(c);
    final_encs.add(temp);
  }

  int finalNum = final_encs.size();
  Encounter[] encs2 = new Encounter[finalNum];
  for (int q = 0; q < finalNum; q++) {
    encs2[q] = (Encounter) final_encs.get(q);
  }
  EncounterDateComparator dc = new EncounterDateComparator(reverse);
  Arrays.sort(encs2, dc);
  return encs2;
}
  
  /**
   * Returns any additional, general comments recorded for this Occurrence as a whole.
   *
   * @return a String of comments
   */
  public String getComments() {
    if (comments != null) {

      return comments;
    } else {
      return "None";
    }
  }
  
  /**
   * Adds any general comments recorded for this Occurrence as a whole.
   *
   * @return a String of comments
   */
  public void addComments(String newComments) {
    if ((comments != null) && (!(comments.equals("None")))) {
      comments += newComments;
    } else {
      comments = newComments;
    }
  }
  
  public Vector returnEncountersWithGPSData(boolean useLocales, boolean reverseOrder) {
    //if(unidentifiableEncounters==null) {unidentifiableEncounters=new Vector();}
    Vector haveData=new Vector();
    Encounter[] myEncs=getDateSortedEncounters(reverseOrder);
    
    Properties localesProps = new Properties();
    if(useLocales){
      try {
        localesProps.load(ShepherdPMF.class.getResourceAsStream("/bundles/locales.properties"));
      } 
      catch (Exception ioe) {
        ioe.printStackTrace();
      }
    }
    
    for(int c=0;c<myEncs.length;c++) {
      Encounter temp=myEncs[c];
      if((temp.getDWCDecimalLatitude()!=null)&&(temp.getDWCDecimalLongitude()!=null)) {
        haveData.add(temp);
      }
      else if(useLocales && (temp.getLocationID()!=null) && (localesProps.getProperty(temp.getLocationID())!=null)){
        haveData.add(temp); 
      }

      }

    return haveData;

  }
  
  
  public String getDWCDateLastModified() {
    return modified;
  }

  public void setDWCDateLastModified(String lastModified) {
    modified = lastModified;
  }
  
  public String getLocationID(){return locationID;}
  
  public void setLocationID(String newLocID){this.locationID=newLocID;}
  
}
