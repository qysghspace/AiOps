Page({
  data: {
    username: '',
    incidents: []
  },

  onShow() {
    const app = getApp();
    this.setData({ username: app.globalData.username || '' });
    this.loadIncidents();
  },

  loadIncidents() {
    const app = getApp();
    const username = app.globalData.username;
    if (!username) {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }

    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/mobile/incidents/my?username=${encodeURIComponent(username)}`,
      method: 'GET',
      success: (resp) => {
        const body = resp.data || {};
        if (body.code !== '0') {
          wx.showToast({ title: body.message || '查询失败', icon: 'none' });
          return;
        }
        this.setData({ incidents: body.data || [] });
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  updateStatus(e) {
    const app = getApp();
    const id = e.currentTarget.dataset.id;
    const status = e.currentTarget.dataset.status;
    const username = app.globalData.username;

    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/mobile/incidents/${id}/status?username=${encodeURIComponent(username)}`,
      method: 'PATCH',
      header: { 'content-type': 'application/json' },
      data: { status },
      success: (resp) => {
        const body = resp.data || {};
        if (body.code !== '0') {
          wx.showToast({ title: body.message || '更新失败', icon: 'none' });
          return;
        }
        wx.showToast({ title: '状态已更新', icon: 'success' });
        this.loadIncidents();
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  goHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  }
});
