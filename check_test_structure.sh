#!/bin/bash

# 检查测试文件结构
echo "检查测试文件结构..."
echo "=================================="

# 检查测试文件是否存在
if [ -f "src/test/java/cn/ncw/music/stream/StreamAudioPlayerTest.java" ]; then
    echo "✓ StreamAudioPlayerTest.java 存在"
else
    echo "✗ StreamAudioPlayerTest.java 不存在"
fi

# 检查测试文件内容
echo "=================================="
echo "测试文件内容概览:"
echo "=================================="

# 统计测试方法数量
test_count=$(grep -c "@Test" src/test/java/cn/ncw/music/stream/StreamAudioPlayerTest.java)
echo "测试方法数量: $test_count"

# 列出测试方法
echo "=================================="
echo "测试方法列表:"
echo "=================================="
grep -n "@Test" src/test/java/cn/ncw/music/stream/StreamAudioPlayerTest.java | cut -d: -f2- | sed 's/@Test//g' | sed 's/void //g' | sed 's/()//g'

echo "=================================="
echo "测试文件结构检查完成!"
