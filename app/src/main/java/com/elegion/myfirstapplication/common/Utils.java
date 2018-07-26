package com.elegion.myfirstapplication.common;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Utils {

    /**
     * Сравинть две коллекции
     */
    public static <T> boolean isCollectionEquals(Collection<T> first, Collection<T> second) {
        if (first == second || (first == null && second.isEmpty()) || (second == null && first.isEmpty()))
            return true;

        if (first.size() != second.size())
            return false;

        final List<T> listSecond = new ArrayList<>(second);

        for (T item : first) {
            Iterator<T> it = listSecond.iterator();
            boolean flag = false;
            while (it.hasNext()) {
                if (item.equals(it.next())) {
                    it.remove();
                    flag = true;
                    break;
                }
            }
            if (!flag) return false;
        }
        return true;
    }

    /**
     * Скрыть клавиатуру
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
