/*
 * The Shepherd Project - A Mark-Recapture Framework
 * Copyright (C) 2011 Jason Holmberg
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.ecocean.servlet;


import org.ecocean.Encounter;
import org.ecocean.Shepherd;
import org.ecocean.User;
import org.ecocean.Util;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.api.rest.RESTUtils;


public class promethuesMetrics extends HttpServlet {

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }


  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }


  private void setDateLastModified(Encounter enc) {
    String strOutputDateTime = ServletUtilities.getDate();
    enc.setDWCDateLastModified(strOutputDateTime);
  }


  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String context="context0";
    context=ServletUtilities.getContext(request);
    Shepherd myShepherd = new Shepherd(context);
    myShepherd.setAction("prometheusMetrics.class");
    //set up for response
    response.setContentType("application/json");
 
    myShepherd.beginDBTransaction();

    try
    {
        myShepherd.beginDBTransaction();
        collectUserMetrics(myShepherd); 
        collectEncounterMetrics(myShepherd);

    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        myShepherd.rollbackDBTransaction();
    }
    
    myShepherd.closeDBTransaction();
  }

  public void collectUserMetrics(Shepherd ms)
  { 
      List<User> myUserList = ms.getAllUsers(); //all users for flukebook
      //with login privileges..
      //without login priveleges
      //active  
  }

  public void collectEncounterMetrics(Shepherd ms)
  {
      List myOccurances = ms.getAllOccurrencesNoQuery();
      ArrayList<Encounters> myEncounters = new ArrayList<>();
      for(int i = 0; i < myOccurances.length(); i++ )
      {
          myEncounters.add(myOccurances.get(i));
      }
  }
}
  
  
