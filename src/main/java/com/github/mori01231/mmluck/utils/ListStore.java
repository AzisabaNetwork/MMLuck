package com.github.mori01231.mmluck.utils;

import java.io.*;
import java.util.ArrayList;

public class ListStore {

    private File storageFile;
    private ArrayList<String> values;

    public ListStore(File file){
        this.storageFile = file;
        this.values = new ArrayList<String>();

        if(this.storageFile.exists() == false){
            try {
                this.storageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void Load(){
        try {
            DataInputStream input = new DataInputStream(new FileInputStream(this.storageFile));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line;

            while ((line = reader.readLine()) != null){
                if (this.Contains(line) == false){
                    this.values.add(line);
                }
            }

            reader.close();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Save(){
        try {
            FileWriter stream = new FileWriter(this.storageFile);
            BufferedWriter out = new BufferedWriter(stream);

            for (String value : this.values){
                out.write(value);
                out.newLine();
            }

            out.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean Contains(String value){
        return this.values.contains(value);
    }

    public String voteContents(String uuid){

        // Check current votes to see if this person has voted yet
        for(String temp: this.values){
            // If the player has vote, return the vote.
            if (temp.startsWith(uuid)){
                return(temp);
            }
        }

        // If there isn't a vote yet
        return("No vote");
    }

    public void Add(String value){
        if(this.Contains(value) == false){
            this.values.add(value);
        }
    }

    public void Remove(String value){
        this.values.remove(value);
    }

    public ArrayList<String> getValues(){
        return this.values;
    }

}