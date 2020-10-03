package com.example.snapchatclone;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;

public class SnapchatAmplifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());

            //Log.i("AmplifyInit", "Initialized Amplify");
        } catch (AmplifyException error) {
            //Log.e("AmplifyInit", "Could not initialize Amplify", error);
        }

    }

}
