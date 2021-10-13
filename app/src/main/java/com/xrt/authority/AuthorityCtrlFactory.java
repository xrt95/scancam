package com.xrt.authority;

import android.app.Activity;

public class AuthorityCtrlFactory {

    public static AuthorityController createAuthorityCtrl(Activity activity){
        return new AuthorityCtrImp(activity);
    }

}
