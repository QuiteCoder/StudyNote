背景：语音助手切换系统语言，app需要根据当前系统的Language更新页面所有的文本

设计思路：在fragment的基类中onConfigurationChanged方法中调用findTextViewAndRefresh获取当前页面中所有EditText和TextView的textResId和hintResId，再从新给他们setText、setHint





```
public class BaseFragment extends Fragment {
	@Override
	public void onResume() {
		if (!getActivity().getCurrentLanguage().equals(mCurrentLanguage)) {
			findTextViewAndRefresh(mContentView)
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (!getActivity().getCurrentLanguage().equals(mCurrentLanguage)) {
			findTextViewAndRefresh(mContentView)
		}
	}
}


// viewGroup： Activity或者fragment的根布局
private void findTextViewAndRefresh(ViewGroup viewGroup) {
    if (viewGroup == null) {
        return;
    }
    int childCount = viewGroup.getChildCount();
    for (int i = 0; i < childCount; i ++ ) {
        View childView = viewGroup.getChildAt(i);
        Log.d(TAG, "[hpf]findTextViewAndRefresh: childView = " + childView);
        if (childView instanceof ViewGroup) {
            findTextViewAndRefresh((ViewGroup) childView);
        } else if (childView instanceof android.widget.TextView) {
            // EditText继承于TextView
            // TextView包含mTextId和mHintId
            CharSequence text = ((TextView) childView).getText();
            CharSequence hint = ((TextView) childView).getHint();
            int textRes = 0;
            int hintRes = 0;
            Log.d(TAG, "[hpf]findTextViewAndRefresh: text = " +text + " hint = " + hint);
            if (text != null && !"".equals(text.toString())) {
                textRes = getTextViewTextId(((TextView) childView));
            } else if (hint != null && !"".equals(hint.toString())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hintRes = getTextViewHintId(((TextView) childView));
                } else {
                    localeChangeAndSetHint();
                }
            } else {
                continue;
            }
            Log.d(TAG, "[hpf]findTextViewAndRefresh: textRes = " +textRes + " hintRes = " + hintRes);
            if (textRes > 0 && (text != null && !"".equals(text.toString()))) {
                ((TextView) childView).setText(textRes);
            }
            if (hintRes > 0 && (hint != null && !"".equals(hint.toString()))) {
                ((TextView) childView).setHint(hintRes);
            }
        }
    }
}

/**
 * Android 11 以下版本需要手动更新TextView 的 hint
 * */
private void localeChangeAndSetHint() {
}

private int getTextViewTextId(View textView) {
    try {
        // 获取字节码文件对象
        Class c = Class.forName("android.widget.TextView");
        Field field = c.getDeclaredField("mTextId");
        field.setAccessible(true);
        return field.getInt(textView);
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
}

private int getTextViewHintId(View textView) {
    try {
        // 获取字节码文件对象
        Class c = Class.forName("android.widget.TextView");
        Field field = c.getDeclaredField("mHintId");
        field.setAccessible(true);
        return field.getInt(textView);
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
}
```