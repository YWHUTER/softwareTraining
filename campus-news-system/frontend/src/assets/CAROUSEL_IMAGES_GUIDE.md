# 轮播图片使用指南

## 图片存放位置
将你的轮播图片放入此文件夹：`/frontend/src/assets/`

## 推荐的图片规格
- **格式**：JPG 或 PNG
- **分辨率**：建议 1920x600 或更宽（会自动裁剪适配）
- **文件大小**：建议不超过 500KB（优化加载速度）

## 图片命名建议

### 方案一：序号命名
```
carousel-1.jpg  # 第一张轮播图
carousel-2.jpg  # 第二张轮播图
carousel-3.jpg  # 第三张轮播图
carousel-4.jpg  # 第四张轮播图
```

### 方案二：描述性命名
```
carousel-news.jpg      # 新闻中心
carousel-academic.jpg  # 学术前沿
carousel-campus.jpg    # 校园生活
carousel-notice.jpg    # 通知公告
```

## 更新代码步骤

1. 将图片放入 `/frontend/src/assets/` 文件夹

2. 打开 `/frontend/src/views/Home.vue` 文件

3. 找到 `carouselItems` 数组（约第295行）

4. 更新每个项目的 `image` 路径：

```javascript
const carouselItems = ref([
  {
    id: 1,
    title: '校园新闻中心',
    subtitle: '第一时间了解校园动态，掌握最新资讯',
    image: '/src/assets/carousel-1.jpg'  // 更新为你的图片名
  },
  {
    id: 2,
    title: '学术前沿',
    subtitle: '探索知识边界，引领学术潮流',
    image: '/src/assets/carousel-2.jpg'  // 更新为你的图片名
  },
  // ... 继续更新其他项
])
```

## 注意事项

1. **路径格式**：必须以 `/src/assets/` 开头
2. **大小写敏感**：文件名大小写要完全匹配
3. **支持格式**：.jpg, .jpeg, .png, .gif, .webp
4. **热更新**：保存后会自动刷新预览

## 当前已有图片
- home-bg.jpg
- main-bg.jpg
- login-bg.jpg
- whut-logo.png

你可以先使用这些图片测试效果，再替换为你自己的图片。
