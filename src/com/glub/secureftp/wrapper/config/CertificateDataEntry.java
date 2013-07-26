
//*****************************************************************************
//*
//* (c) Copyright 2002. Glub Tech, Incorporated. All Rights Reserved.
//*
//* $Id: CertificateDataEntry.java 39 2009-05-11 22:50:09Z gary $
//*
//*****************************************************************************

package com.glub.secureftp.wrapper.config;

import com.glub.secureftp.wrapper.*;
import com.glub.secureftp.common.*;

import java.io.*;
import java.util.*;

public class CertificateDataEntry {
  private PrintStream out = System.out;
  private BufferedReader stdin = 
                           new BufferedReader(new InputStreamReader(System.in));
  private ServerInfo info = null;

  private String commonName = "";
  private String organization = "";
  private String unit = "";
  private String city = "";
  private String state = "";
  private String country = "";
  private int    days = 1025;

  private String caCertPath = "";
  private String privateKeyPath = "";
  private String publicCertPath = "";

  public CertificateDataEntry( ServerInfo info ) {
    this.info = info;
  }

  public void getData() throws CommandException { 
    boolean boolResult = false;

    ArrayList response = new ArrayList(1);

    try {
      if ( getBoolean("Do you want to generate a certificate?",
                      getYN(true), response) ) {
        boolResult = ((Boolean)response.get(0)).booleanValue();
        
      }

      if ( boolResult ) {
        getCN();
        getO();
        getOU();
        getCity();
        getState();
        getCountry();
        getDays();
        generateCert();
     }
     else {
       getPrivateKey();
       getPublicCert();
       getCACerts();

       File privFile = new File( privateKeyPath );
       File pubFile = new File( publicCertPath );

       if ( privFile.exists() && pubFile.exists() ) {
         info.setPublicCert( pubFile );
         info.setPrivateKey( privFile );

         if ( caCertPath != null ) {
           info.setCACertsFromString( caCertPath );
         }
 
         try {
           ConfigurationManager.getInstance().writeConfiguration();
         } 
         catch ( IOException ioe ) { 
           System.out.println("Problem writing configuration: " + 
                              ioe.getMessage());
         }
       } 
       else {
           System.out.println("At least one of the files cannot be found.");
       }
     }
    }
    catch ( IOException ioe ) {
    }
  }

  protected void generateCert() {
    String certPrefix = info.getServerID();

    if ( info.getExplicitEnabled() ) {
      certPrefix += "_" + info.getExplicitPort(); 
    }

    if ( info.getImplicitEnabled() ) {
      certPrefix += "_" + info.getImplicitPort(); 
    }

    File configDir = new File( System.getProperty("user.dir") );
    File publicCertFile = new File( configDir, certPrefix + "_certificate.der" ); 
    File privateKeyFile = new File( configDir, certPrefix + "_private.pk8" );

    CertInfo certInfo = 
      new CertInfo( commonName, organization, unit, city, state, country );

    boolean certGenerated = 
      KeyUtil.writeCertAndKey( certInfo, days, publicCertFile, privateKeyFile );

    if ( certGenerated ) {
      info.setPublicCert( publicCertFile );
      info.setPrivateKey( privateKeyFile );

      try {
        ConfigurationManager.getInstance().writeConfiguration();
      } 
      catch ( IOException ioe ) { 
        System.out.println("Problem writing configuration: " + 
                           ioe.getMessage());
      }
    }
  }

  protected void getCACerts() throws IOException {
    ArrayList response = new ArrayList(1);

    String def = "";
    if ( info.getCACertsAsString() != null) {
      def = info.getCACertsAsString();
    }

    getResponse("Absolute path to the CA cert", def, response); 

    caCertPath = ((String)response.get(0)).toString();
  }

  protected void getPrivateKey() throws IOException {
    ArrayList response = new ArrayList(1);

    String def = "";
    if ( info.getPrivateKey() != null) {
      def = info.getPrivateKey().getAbsolutePath();
    }

    getResponse("Absolute path to Private key", def, response); 
    String fileStr = ((String)response.get(0)).toString();
    File file = new File( fileStr );
    if ( !file.exists() ) {
      System.out.println( "WARNING: File not found." );
    }
    privateKeyPath = fileStr;
  }

  protected void getPublicCert() throws IOException {
    ArrayList response = new ArrayList(1);

    String def = "";
    if ( info.getPublicCert() != null) {
      def = info.getPublicCert().getAbsolutePath();
    }

    getResponse("Absolute path to Public cert", def, response); 

    String fileStr = ((String)response.get(0)).toString();
    File file = new File( fileStr );
    if ( !file.exists() ) {
      System.out.println( "WARNING: File not found." );
    }
    publicCertPath = fileStr;
  }

  protected void getCN() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter the hostname", info.getServerID(), response); 
    commonName = ((String)response.get(0)).toString();
  }

  protected void getO() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter your company's name", response); 
    organization = ((String)response.get(0)).toString();
  }

  protected void getOU() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter your department", response); 
    unit = ((String)response.get(0)).toString();
  }

  protected void getCity() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter your city", response); 
    city = ((String)response.get(0)).toString();
  }

  protected void getState() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter your state", response); 
    state = ((String)response.get(0)).toString();
  }

  protected void getCountry() throws IOException {
    ArrayList response = new ArrayList(1);

    getResponse("Enter your country", response); 
    country = ((String)response.get(0)).toString();
  }

  protected void getDays() throws IOException {
    ArrayList response = new ArrayList(1);

    getInteger("Enter number of days cert is valid", "1025", response); 
    days = ((Integer)response.get(0)).intValue();
  }


  protected boolean getResponse( String question, 
                                 ArrayList response ) throws IOException {
    return getResponse( question, "", response );
  }

  protected boolean getResponse( String question, String def,
                                 ArrayList response ) throws IOException {
    boolean result = false;

    response.clear();

    if ( null != def )
      out.print(question + " [" + def + "]: ");
    else
      out.print(question + ": ");

    String in = stdin.readLine().trim();

    if ( in.length() > 0 )
      result = true;

    if ( in.length() == 0 ) {
      in = def;
    }

    response.add( in );

    return result;
  }

  protected boolean getInteger( String question, String def,
                                ArrayList response ) throws IOException {
    boolean result = getResponse( question, def, response );

    String responseStr = (String)response.get(0);

    Integer intResult = new Integer(def);
    try {
      intResult = new Integer(responseStr);
    }
    catch ( Exception e ) {}

    response.clear();
    response.add( intResult );

    return result;
  }

  protected boolean getBoolean( String question, String def,
                                ArrayList response ) throws IOException {
    boolean result = getResponse( question, def, response );

    String responseStr = (String)response.get(0);

    boolean boolResult = responseStr.equalsIgnoreCase(def);

    if ( responseStr.equalsIgnoreCase("y") ) {
      boolResult = true;
    }
    else {
      boolResult = false;
    }

    response.clear();
    response.add( new Boolean(boolResult) );

    if ( !result && boolResult ) {
      result = boolResult;
    }

    return result;
  }

  private String getYN( boolean doIt ) {
    if ( doIt )
      return "y";
    else
      return "n";
  }
}

