# AsyncTask

## 一、用途

可以让子线程处理完的结果转到UI线程中，内部封装了线程池和Handler。



## 二、用法

```java
public class TaskActivity extends Activity {

    AsyncTask<Void, Integer, ArrayList<String>> mTask;

    @Override
    protected void onStart() {
        super.onStart();
        startTask();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelTask();
    }

    // 开始任务
    private void startTask() {
        if (mTask != null) {
            mTask.cancel(true);
        }

        mTask = new MyAsyncTask();
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // 取消任务
    private void cancelTask() {
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    /**
     * 自定义异步任务类
     */
    private class MyAsyncTask extends AsyncTask<Void, Integer, ArrayList<String>> {
        @Override
        protected void onPreExecute() {
            // TODO 在这里可以加载LoadingView提示用户
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            // TODO 在这里执行耗时的操作，如网络请求，大量的计算等等
            ArrayList<String> result = new ArrayList<String>();
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(100);
                    // 这个方法是属于AsyncTask的，会触发onProgressUpdate回调
                    publishProgress(i);
                    result.add("" + i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            mTask = null;
            // TODO 在这里进行相应的界面更新逻辑
            // 例如将result加载到ListView中进行显示
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // TODO 将进度更新到界面，本例用用整数表示百分比
        }
    }
}
```





## 3、Q&A

## AsyncTask为什么会被弃用？

`AsyncTask` 被弃用通常是指在Android开发中，`AsyncTask` 类已不再推荐用于执行后台操作，因为从Android P（API 28）开始，`AsyncTask` 的执行策略已有所改变，可能会影响到之前的行为。

**问题解释：**

`AsyncTask` 类用于在后台线程上执行简单的后台操作，并提供简单的方法将结果传递给主线程。但从Android P开始，`AsyncTask` 的执行策略发生了变化，尝试在后台线程中执行耗时操作可能不会按预期工作，因为`AsyncTask` 的行为可能受到线程池的限制。

**Google官方解释：**

AysncTask意图提供简单且合适的UI线程使用，然而，它最主要的使用实例是作为UI的组成部分，会导致内存泄漏，回调遗失或改变配置时崩溃。

