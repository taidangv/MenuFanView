package com.dvtai.fanview;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private FanView fanView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fanView = (FanView) findViewById(R.id.fv_menu);
	}

	public void setAdapter(View view) {
		List<String> menuTitles = new ArrayList<>();
		for (int i = 0; i < 17; i++) {
			menuTitles.add("Item title " + (i + 1));
		}
		MenuAdapter adapter = new MenuAdapter(this, menuTitles);
		fanView.setAdapter(adapter);
		Log.w("TAG_DEV", "onCreate: set adapter");
	}

	public void onOpenMenu(View view) {
		fanView.createOpenMenuAnimator().start();
	}

	public void onCloseMenu(View view) {
		fanView.createCloseMenuAnimator().start();
	}


	static class MenuAdapter extends BaseAdapter {

		private List<String> menuTitles = new ArrayList<>();
		private LayoutInflater layoutInflater;

		public MenuAdapter(Context context, List<String> menuTitles) {
			this.menuTitles = menuTitles;
			this.layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return menuTitles.size();
		}

		@Override
		public String getItem(int position) {
			return menuTitles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return -1; //no.op
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewGroup itemView = (ViewGroup) layoutInflater.inflate(R.layout.item_fan, null);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(v.getContext(), getItem(position), Toast.LENGTH_SHORT).show();
				}
			});
			TextView tv = (TextView) itemView.findViewById(R.id.tv_name);
			tv.setText(menuTitles.get(position));
			return itemView;
		}
	}
}
