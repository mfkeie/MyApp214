package com.elegion.myfirstapplication.comments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.elegion.myfirstapplication.R;
import com.elegion.myfirstapplication.model.Comment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommentsHolder extends RecyclerView.ViewHolder {

    private TextView mAuthor;
    private TextView mText;
    private TextView mTimeStamp;
    DateFormat df_in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    DateFormat df_out_full = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    DateFormat df_out_short = new SimpleDateFormat("HH:mm:ss");

    public CommentsHolder(View itemView) {
        super(itemView);
        mAuthor = itemView.findViewById(R.id.tv_author);
        mText = itemView.findViewById(R.id.tv_text);
        mTimeStamp = itemView.findViewById(R.id.tv_timestamp);
    }

    public void bind(Comment item) {
        mAuthor.setText(/*item.getId() + ": " + */item.getAuthor());
        mText.setText(item.getText());
        try {
            Date date = df_in.parse(item.getTimestamp());
            if((new Date()).getTime() - date.getTime() < 1000*60*60*24)
                mTimeStamp.setText(df_out_short.format(date));
            else
                mTimeStamp.setText(df_out_full.format(date));
        } catch (Exception ex) {
            //Если не разберем, то выводим что получили
            mTimeStamp.setText(item.getTimestamp());
            ex.printStackTrace();
        }
    }
}
