package com.example.snapchatclone;

import com.amplifyframework.datastore.generated.model.Message;
import com.amplifyframework.datastore.generated.model.Snap;

import java.io.File;

public class SnapWrapper {
   // Snap snap;
    Message snap;//_message;
    File image_file;
   /* public SnapWrapper(Snap snap,File image_file){
        this.snap = snap;
        this.image_file = image_file;
    }*/

    public SnapWrapper(Message snap, File image_file) {
        this.snap = snap;
        this.image_file = image_file;
    }
}
