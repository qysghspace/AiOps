Page({
  data: {
    question: '',
    answer: ''
  },

  onQuestionInput(e) {
    this.setData({ question: e.detail.value });
  },

  askHelp() {
    const app = getApp();
    const question = (this.data.question || '').trim();
    if (!question) {
      wx.showToast({ title: '请输入问题', icon: 'none' });
      return;
    }

    wx.request({
      url: `${app.globalData.apiBaseUrl}/api/help/ask`,
      method: 'POST',
      header: { 'content-type': 'application/json' },
      data: { question, context: 'miniprogram-help-center' },
      success: (resp) => {
        const body = resp.data || {};
        if (body.code !== '0') {
          wx.showToast({ title: body.message || '问答失败', icon: 'none' });
          return;
        }
        this.setData({ answer: (body.data && body.data.answer) || '' });
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  }
});
