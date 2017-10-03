package com.hch.mine;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * 扫雷游戏
 * 
 * @author hch 20170825
 * 
 */
public class MainActivity extends Activity {
	private final static int mineNum = 10;
	private boolean isClick = false;
	private ImageView faceImg;
	private TableLayout tableLayout;
	private TextView timeTxt, quantityTxt;
	private Button[][] buttons = new Button[9][9];
	private Mine[][] mines = new Mine[9][9];
	private ArrayList<Button> bombs = new ArrayList<Button>();
	private int second = 0;
	private int quantity = mineNum;

	private MediaPlayer mediaPlayer;
	private SoundPool soundPool;
	private int clickSoundID;// 点击音效ID
	private int bombSoundID;// 爆炸音效ID
	private int winSoundID;// 胜利音效ID
	private int defeatSoundID;// 失败音效ID
	private float volumnCurrent;
	private float volumnRatio;

	/**
	 * 定时器，定时更新时间
	 */
	Handler handler = new Handler();
	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (second < 999) {
				handler.postDelayed(this, 1000);
				second++;
			} else {
				handler.removeCallbacks(this);
			}
			timeTxt.setText(String.format("%03d", second));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		timeTxt = (TextView) findViewById(R.id.timeTxt);
		quantityTxt = (TextView) findViewById(R.id.quantityTxt);
		faceImg = (ImageView) findViewById(R.id.faceImg);
		tableLayout = (TableLayout) findViewById(R.id.tableLayout);

		Typeface face = Typeface.createFromAsset(getAssets(),
				"fonts/digifaw.ttf");
		timeTxt.setTypeface(face);
		quantityTxt.setTypeface(face);

		faceImg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new Builder(MainActivity.this);
				builder.setMessage("该游戏操作方式分为点击和长按，单击翻开方格，长按则标记地雷，待将所有地雷都标记出来，你便赢了！");
				builder.setTitle("帮助说明");
				builder.setNegativeButton("关闭",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				builder.create().show();
			}
		});

		initMinePanel();

		AudioManager am = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volumnCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		volumnRatio = volumnCurrent / audioMaxVolumn;

		// initBgMusic();
		initClickSound();
	}

	/**
	 * 雷区按钮点击事件
	 * 
	 */
	private class MineClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (!isClick) {
				handler.postDelayed(runnable, 1000);
				isClick = true;
			}
			Button button = (Button) v;
			Mine mine = (Mine) button.getTag();
			int i = mine.getI(), j = mine.getJ();
			boolean sign = mines[i][j].isSign();
			if (!sign && mine.getValue() == 9) {
				handler.removeCallbacks(runnable);
				faceImg.setImageResource(R.drawable.face_sad);
				bomb();
				playClickSound(bombSoundID);
				showMessage("游戏失败", "你踩到地雷了！");
			} else {
				playClickSound(clickSoundID);
				showMineField(mine);
			}

			checkResult();
		}
	}

	private class MineLongClickListener implements OnLongClickListener {
		@Override
		public boolean onLongClick(View arg0) {
			if (!isClick) {
				handler.postDelayed(runnable, 1000);
				isClick = true;
			}
			Button button = (Button) arg0;
			Mine mine = (Mine) button.getTag();
			int i = mine.getI(), j = mine.getJ();
			boolean show = mines[i][j].isShow();
			boolean sign = mines[i][j].isSign();
			if (!show) {
				if (!sign) {
					mines[i][j].setSign(true);
					quantity--;
					setMineFieldBackground4Sign(buttons[i][j]);
				} else {
					mines[i][j].setSign(false);
					quantity++;
					setMineFieldBackground(buttons[i][j]);
				}
				quantityTxt.setText(String.format("%03d", quantity));
				checkResult();
			}
			return true;
		}
	}

	private void checkResult() {
		int chooseNum = 0;
		int rightNum = 0;
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				if (mines[i][j].isSign()) {
					chooseNum++;
					if (mines[i][j].getValue() == 9) {
						rightNum++;
					}
				}
			}
		}

		if (chooseNum == mineNum && rightNum == mineNum) {
			handler.removeCallbacks(runnable);
			playClickSound(winSoundID);
			showMessage("游戏胜利", "恭喜你赢了！一共用时 " + second + "秒");
		} else if (chooseNum >= mineNum) {
			handler.removeCallbacks(runnable);
			faceImg.setImageResource(R.drawable.face_sad);
			bomb();
			playClickSound(defeatSoundID);
			showMessage("游戏失败", "你没有挖出全部地雷！");
		}
	}

	/**
	 * 爆炸效果
	 */
	private void bomb() {
		for (Button button : bombs) {
			setMineFieldBackground4Bomb(button);
		}
	}

	/**
	 * 初始化雷区界面
	 */
	private void initMinePanel() {
		isClick = false;

		String mineIdxs = initRandomMine();
		for (int i = 0; i < 9; i++) {
			TableRow tableRow = new TableRow(this);
			tableRow.setLayoutParams(new TableLayout.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT,
					TableLayout.LayoutParams.MATCH_PARENT, 1));
			TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
					TableLayout.LayoutParams.MATCH_PARENT,
					TableLayout.LayoutParams.MATCH_PARENT, 1);
			layoutParams.setMargins(0, 0, 0, 0);
			for (int j = 0; j < 9; j++) {
				buttons[i][j] = new Button(this);
				mines[i][j] = new Mine(i, j);
				// buttons[i][j].setText(i + "," + j);
				setMineFieldBackground(buttons[i][j]);
				buttons[i][j].setLayoutParams(layoutParams);
				buttons[i][j].setOnClickListener(new MineClickListener());
				buttons[i][j]
						.setOnLongClickListener(new MineLongClickListener());
				String idx = i + "" + j;
				if (mineIdxs.contains(idx)) {
					mines[i][j].setValue(9);
					bombs.add(buttons[i][j]);
					// buttons[i][j].setText("9");
				} else {
					mines[i][j].setValue(0);
					// buttons[i][j].setText("0");
				}
				buttons[i][j].setTag(mines[i][j]);
				tableRow.addView(buttons[i][j]);
			}
			tableLayout.addView(tableRow);
		}

		initMineNumber();
	}

	/**
	 * 初始化地雷的随机坐标
	 * 
	 * @return
	 */
	private String initRandomMine() {
		StringBuilder sb = new StringBuilder("");
		int x, y;
		for (int i = 0; i < mineNum; i++) {
			x = (int) (Math.random() * 8);
			y = (int) (Math.random() * 8);
			while (true) {
				if (!sb.toString().contains(x + "" + y)) {
					break;
				} else {
					x = (int) (Math.random() * 8) + 1;
					y = (int) (Math.random() * 8) + 1;
				}
			}

			sb.append(x + "" + y + ",");
		}
		return sb.toString();
	}

	/**
	 * 初始化地雷旁边的数字
	 */
	private void initMineNumber() {
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				Mine mine = mines[i][j];
				if (mine.getValue() == 9) {
					// 左上角
					if (i - 1 >= 0 && j - 1 >= 0
							&& !((int) mines[i - 1][j - 1].getValue() == 9)) {
						mines[i - 1][j - 1].setValue((int) mines[i - 1][j - 1]
								.getValue() + 1);
						// buttons[i - 1][j - 1].setText(String
						// .valueOf(mines[i - 1][j - 1].getValue()));
					}
					// 顶部
					if (i - 1 >= 0 && !((int) mines[i - 1][j].getValue() == 9)) {
						mines[i - 1][j].setValue((int) mines[i - 1][j]
								.getValue() + 1);
						// buttons[i - 1][j].setText(String
						// .valueOf(mines[i - 1][j].getValue()));
					}
					// 右上角
					if (i - 1 >= 0 && j + 1 < 9
							&& !((int) mines[i - 1][j + 1].getValue() == 9)) {
						mines[i - 1][j + 1].setValue((int) mines[i - 1][j + 1]
								.getValue() + 1);
						// buttons[i - 1][j + 1].setText(String
						// .valueOf(mines[i - 1][j + 1].getValue()));
					}
					// 左部
					if (j - 1 >= 0 && !((int) mines[i][j - 1].getValue() == 9)) {
						mines[i][j - 1].setValue((int) mines[i][j - 1]
								.getValue() + 1);
						// buttons[i][j - 1].setText(String
						// .valueOf(mines[i][j - 1].getValue()));
					}
					// 右部
					if (j + 1 < 9 && !((int) mines[i][j + 1].getValue() == 9)) {
						mines[i][j + 1].setValue((int) mines[i][j + 1]
								.getValue() + 1);
						// buttons[i][j + 1].setText(String
						// .valueOf(mines[i][j + 1].getValue()));
					}
					// 左下角
					if (i + 1 < 9 && j - 1 >= 0
							&& !((int) mines[i + 1][j - 1].getValue() == 9)) {
						mines[i + 1][j - 1].setValue((int) mines[i + 1][j - 1]
								.getValue() + 1);
						// buttons[i + 1][j - 1].setText(String
						// .valueOf(mines[i + 1][j - 1].getValue()));
					}
					// 顶部
					if (i + 1 < 9 && !((int) mines[i + 1][j].getValue() == 9)) {
						mines[i + 1][j].setValue((int) mines[i + 1][j]
								.getValue() + 1);
						// buttons[i + 1][j].setText(String
						// .valueOf(mines[i + 1][j].getValue()));
					}
					// 右下角
					if (i + 1 < 9 && j + 1 < 9
							&& !((int) mines[i + 1][j + 1].getValue() == 9)) {
						mines[i + 1][j + 1].setValue((int) mines[i + 1][j + 1]
								.getValue() + 1);
						// buttons[i + 1][j + 1].setText(String
						// .valueOf(mines[i + 1][j + 1].getValue()));
					}
				}
			}
		}
	}

	/**
	 * 显示雷区的方格
	 */
	private void showMineField(Mine mine) {
		int i = mine.getI(), j = mine.getJ(), value = mine.getValue();
		boolean show = mines[i][j].isShow();
		boolean sign = mines[i][j].isSign();
		if (!show && !sign) {
			mines[i][j].setShow(true);
			setMineFieldBackground4Show(buttons[i][j]);
			buttons[i][j].setText(value == 0 ? "" : String.valueOf(value));
		}
		if (!sign && value == 0) {
			// 左上角
			if (i - 1 >= 0 && j - 1 >= 0
					&& !((int) mines[i - 1][j - 1].getValue() == 9)
					&& !mines[i - 1][j - 1].isShow()) {
				mines[i - 1][j - 1].setShow(true);
				setMineFieldBackground4Show(buttons[i - 1][j - 1]);
				buttons[i - 1][j - 1]
						.setText(mines[i - 1][j - 1].getValue() == 0 ? ""
								: String.valueOf(mines[i - 1][j - 1].getValue()));
				showMineField(mines[i - 1][j - 1]);
			}
			// 顶部
			if (i - 1 >= 0 && !((int) mines[i - 1][j].getValue() == 9)
					&& !mines[i - 1][j].isShow()) {
				mines[i - 1][j].setShow(true);
				setMineFieldBackground4Show(buttons[i - 1][j]);
				buttons[i - 1][j].setText(mines[i - 1][j].getValue() == 0 ? ""
						: String.valueOf(mines[i - 1][j].getValue()));
				showMineField(mines[i - 1][j]);
			}
			// 右上角
			if (i - 1 >= 0 && j + 1 < 9
					&& !((int) mines[i - 1][j + 1].getValue() == 9)
					&& !mines[i - 1][j + 1].isShow()) {
				mines[i - 1][j + 1].setShow(true);
				setMineFieldBackground4Show(buttons[i - 1][j + 1]);
				buttons[i - 1][j + 1]
						.setText(mines[i - 1][j + 1].getValue() == 0 ? ""
								: String.valueOf(mines[i - 1][j + 1].getValue()));
				showMineField(mines[i - 1][j + 1]);
			}
			// 左部
			if (j - 1 >= 0 && !((int) mines[i][j - 1].getValue() == 9)
					&& !mines[i][j - 1].isShow()) {
				mines[i][j - 1].setShow(true);
				setMineFieldBackground4Show(buttons[i][j - 1]);
				buttons[i][j - 1].setText(mines[i][j - 1].getValue() == 0 ? ""
						: String.valueOf(mines[i][j - 1].getValue()));
				showMineField(mines[i][j - 1]);
			}
			// 右部
			if (j + 1 < 9 && !((int) mines[i][j + 1].getValue() == 9)
					&& !mines[i][j + 1].isShow()) {
				mines[i][j + 1].setShow(true);
				setMineFieldBackground4Show(buttons[i][j + 1]);
				buttons[i][j + 1].setText(mines[i][j + 1].getValue() == 0 ? ""
						: String.valueOf(mines[i][j + 1].getValue()));
				showMineField(mines[i][j + 1]);
			}
			// 左下角
			if (i + 1 < 9 && j - 1 >= 0
					&& !((int) mines[i + 1][j - 1].getValue() == 9)
					&& !mines[i + 1][j - 1].isShow()) {
				mines[i + 1][j - 1].setShow(true);
				setMineFieldBackground4Show(buttons[i + 1][j - 1]);
				buttons[i + 1][j - 1]
						.setText(mines[i + 1][j - 1].getValue() == 0 ? ""
								: String.valueOf(mines[i + 1][j - 1].getValue()));
				showMineField(mines[i + 1][j - 1]);
			}
			// 顶部
			if (i + 1 < 9 && !((int) mines[i + 1][j].getValue() == 9)
					&& !mines[i + 1][j].isShow()) {
				mines[i + 1][j].setShow(true);
				setMineFieldBackground4Show(buttons[i + 1][j]);
				buttons[i + 1][j].setText(mines[i + 1][j].getValue() == 0 ? ""
						: String.valueOf(mines[i + 1][j].getValue()));
				showMineField(mines[i + 1][j]);
			}
			// 右下角
			if (i + 1 < 9 && j + 1 < 9
					&& !((int) mines[i + 1][j + 1].getValue() == 9)
					&& !mines[i + 1][j + 1].isShow()) {
				mines[i + 1][j + 1].setShow(true);
				setMineFieldBackground4Show(buttons[i + 1][j + 1]);
				buttons[i + 1][j + 1]
						.setText(mines[i + 1][j + 1].getValue() == 0 ? ""
								: String.valueOf(mines[i + 1][j + 1].getValue()));
				showMineField(mines[i + 1][j + 1]);
			}
		}
	}

	/**
	 * 设置地雷背景
	 * 
	 * @param button
	 */
	private void setMineFieldBackground(Button button) {
		// GradientDrawable drawable = new GradientDrawable();
		// drawable.setShape(GradientDrawable.RECTANGLE); // 画框
		// drawable.setStroke(2, Color.BLACK); // 边框粗细及颜色
		// drawable.setColor(0xff8470FF); // 边框内部颜色
		// button.setBackground(drawable);

		button.setBackgroundResource(R.drawable.grid1);
	}

	/**
	 * 设置选择地雷背景
	 * 
	 * @param button
	 */
	private void setMineFieldBackground4Show(Button button) {
		// GradientDrawable drawable = (GradientDrawable)
		// button.getBackground();
		// drawable.setShape(GradientDrawable.RECTANGLE); // 画框
		// drawable.setStroke(2, Color.BLACK); // 边框粗细及颜色
		// drawable.setColor(0xffCCCCCC); // 边框内部颜色
		// button.setBackground(drawable);

		button.setBackgroundResource(R.drawable.grid2);
	}

	/**
	 * 设置标记地雷背景
	 * 
	 * @param button
	 */
	private void setMineFieldBackground4Sign(Button button) {
		// GradientDrawable drawable = (GradientDrawable)
		// button.getBackground();
		// drawable.setShape(GradientDrawable.RECTANGLE); // 画框
		// drawable.setStroke(2, Color.BLACK); // 边框粗细及颜色
		// drawable.setColor(0xffB0E2FF); // 边框内部颜色
		// button.setBackground(drawable);

		button.setBackgroundResource(R.drawable.grid3);
	}

	/**
	 * 设置地雷爆炸背景
	 * 
	 * @param button
	 */
	private void setMineFieldBackground4Bomb(Button button) {
		// GradientDrawable drawable = (GradientDrawable)
		// button.getBackground();
		// drawable.setShape(GradientDrawable.RECTANGLE); // 画框
		// drawable.setStroke(2, Color.BLACK); // 边框粗细及颜色
		// drawable.setColor(0xffB0E2FF); // 边框内部颜色
		// button.setBackground(drawable);

		button.setBackgroundResource(R.drawable.grid4);
	}

	/**
	 * 显示信息框
	 * 
	 * @param title
	 * @param message
	 */
	private void showMessage(String title, String message) {
		AlertDialog.Builder builder = new Builder(MainActivity.this);
		builder.setMessage(message);
		builder.setTitle(title);
		builder.setPositiveButton("重新开始",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						resetTime();
						tableLayout.removeAllViews();
						initMinePanel();
					}
				});
		builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				System.exit(0);
			}
		});
		builder.create().show();
	}

	/**
	 * 返回按键
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AlertDialog.Builder builder = new Builder(MainActivity.this);
			builder.setMessage("是否退出游戏？");
			builder.setTitle("提示");
			builder.setPositiveButton("重新开始",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							handler.removeCallbacks(runnable);
							resetTime();
							tableLayout.removeAllViews();
							initMinePanel();
						}
					});
			builder.setNegativeButton("退出",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							System.exit(0);
						}
					});
			builder.create().show();
		}
		return true;
	}

	/**
	 * 重置定时器
	 */
	private void resetTime() {
		second = 0;
		timeTxt.setText("000");
		quantityTxt.setText("000");
		quantity = mineNum;
		faceImg.setImageResource(R.drawable.face_smile);
	}

	/**
	 * 初始化背景音乐
	 */
	private void initBgMusic() {
		mediaPlayer = MediaPlayer.create(this, R.raw.bg);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setVolume(volumnRatio, volumnRatio);
		mediaPlayer.setLooping(true);
		mediaPlayer.start();
	}

	/**
	 * 初始化点击音效
	 */
	private void initClickSound() {
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
		clickSoundID = soundPool.load(this, R.raw.click, 1);
		bombSoundID = soundPool.load(this, R.raw.bomb, 2);
		winSoundID = soundPool.load(this, R.raw.win, 2);
		defeatSoundID = soundPool.load(this, R.raw.defeat, 2);
	}

	/**
	 * 播放点击音效
	 */
	private void playClickSound(int soundID) {
		soundPool.play(soundID, volumnRatio, // 左耳道音量【0~1】
				volumnRatio, // 右耳道音量【0~1】
				0, // 播放优先级【0表示最低优先级】
				1, // 循环模式【0表示循环一次，-1表示一直循环，其他表示数字+1表示当前数字对应的循环次数】
				1 // 播放速度【1是正常，范围从0~2】
				);
	}

	@Override
	protected void onDestroy() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		super.onDestroy();
	}

}
