/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client;

import games.stendhal.client.gui.*;
import marauroa.common.*;

public class stendhal extends Thread
  {
  public static boolean doLogin=false;
  
  final public static boolean showCollisionDetection=false;
  final public static boolean showEveryoneAttackInfo=false;
  final public static boolean showEveryoneXPInfo=false;
  
  final public static String VERSION="0.31";
  
  public static void main(String args[]) 
    {
    
    Log4J.init("games/stendhal/log4j.properties");
    
    for(int i=0;i<args.length;i++)
      {
      if(args[i].equals("-l"))
        {
        Logger.setAllowed(new String[]{"*"});
        }
      }

    StendhalClient client=StendhalClient.get();
    new StendhalFirstScreen(client);
    
    while(!doLogin)
      {
      try{Thread.sleep(200);}catch(Exception e){}
      }
    
    new j2DClient(client);
    }    
  }
