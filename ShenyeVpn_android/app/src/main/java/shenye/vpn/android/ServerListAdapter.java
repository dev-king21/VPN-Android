package shenye.vpn.android;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerListAdapter extends ArrayAdapter {

    String[] spinnerTitles;
    String[] spinnerImages;
    String[] spinnerPingTimes;
    Context mContext;

    private static class ViewHolder {
        ImageView mFlag;
        TextView mLocation;
        TextView mPingtime;
    }

    public ServerListAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    public ServerListAdapter(@NonNull Context context, String[] titles, String[] images, String[] times) {
        super(context, R.layout.spinner_item);
        this.spinnerTitles = titles;
        this.spinnerImages = images;
        this.spinnerPingTimes = times;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return spinnerTitles.length;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder mViewHolder = new ViewHolder();
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.spinner_item, parent, false);
            mViewHolder.mFlag = (ImageView) convertView.findViewById(R.id.flag_server);
            mViewHolder.mLocation = (TextView) convertView.findViewById(R.id.location_server);
            mViewHolder.mPingtime = (TextView) convertView.findViewById(R.id.pingtime_server);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        int resID = mContext.getResources().getIdentifier(spinnerImages[position].toLowerCase(), "drawable",  mContext.getPackageName());
        mViewHolder.mFlag.setImageResource(resID);
        mViewHolder.mLocation.setText(spinnerTitles[position]);
        mViewHolder.mPingtime.setText(spinnerPingTimes[position]);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }


}
