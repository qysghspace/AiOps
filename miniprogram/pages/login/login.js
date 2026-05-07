Page({
  data: {
    username: ''
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value });
  },

  onWxLogin() {
    const username = (this.data.username || '').trim();
    if (!username) {
      wx.showToast({ title: '请输入用户名', icon: 'none' });
      return;
    }

    const app = getApp();
    wx.login({
      success: (res) => {
        const code = res.code || 'dev-code';
        wx.request({
          url: `${app.globalData.apiBaseUrl}/api/mobile/auth/wx-login`,
          method: 'POST',
          header: { 'content-type': 'application/json' },
          data: { code, username },
          success: (resp) => {
            const body = resp.data || {};
            if (body.code !== '0') {
              wx.showToast({ title: body.message || '登录失败', icon: 'none' });
              return;
            }
            app.globalData.username = body.data.username;
            app.globalData.token = body.data.token;
            wx.showToast({ title: '登录成功', icon: 'success' });
            wx.redirectTo({ url: '/pages/incidents/incidents' });
          },
          fail: () => {
            wx.showToast({ title: '无法连接后端', icon: 'none' });
          }
        });
      },
      fail: () => {
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      }
    });
  }
});
