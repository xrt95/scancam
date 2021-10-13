package com.xrt.authority;

import android.view.ViewGroup;

public interface AuthorityController {
    boolean lackPermission(String permission);
    boolean lackPermissions(String[] permission);
    boolean validPermissionsWithHint(String[] permissions, int requestCode);
    void requestPermissions(String[] permissions, int requestCode);
}
