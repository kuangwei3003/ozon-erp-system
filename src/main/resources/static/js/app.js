/**
 * Ozon ERP 前端通用脚本：统一 fetch 封装、提示条、同步按钮
 * 约定：后端返回 { code, message, data }；请求体字段使用 snake_case（与 Ozon 报文一致）
 */
window.erp = (function () {

    function toast(message, kind) {
        const el = document.getElementById('erp-toast');
        if (!el) {
            alert(message);
            return;
        }
        el.textContent = message;
        el.className = 'toast show ' + (kind || '');
        clearTimeout(el._timer);
        el._timer = setTimeout(() => { el.className = 'toast ' + (kind || ''); }, 4200);
    }

    async function request(url, options) {
        const opts = options || {};
        if (opts.body !== undefined && typeof opts.body !== 'string') {
            opts.body = JSON.stringify(opts.body);
            opts.headers = Object.assign({'Content-Type': 'application/json'}, opts.headers || {});
        }
        opts.method = opts.method || (opts.body ? 'POST' : 'GET');
        const resp = await fetch(url, opts);
        let json;
        try {
            json = await resp.json();
        } catch (e) {
            throw new Error('接口返回非 JSON（HTTP ' + resp.status + '）');
        }
        if (json.code !== 0) {
            throw new Error(json.message || ('请求失败 HTTP ' + resp.status));
        }
        return json.data;
    }

    const get = (url) => request(url);
    /** POST 总是携带 JSON 请求体（无体时传空对象），避免被误判为 GET */
    const post = (url, body) => request(url, {method: 'POST', body: body == null ? {} : body});

    /**
     * 通用同步/提交按钮：按钮置灰 -> 调接口 -> 提示 -> 可选刷新页面
     */
    async function run(button, url, body, options) {
        const opts = options || {};
        const old = button.textContent;
        button.disabled = true;
        button.textContent = opts.busyText || '处理中…';
        try {
            const data = await post(url, body);
            if (opts.render) {
                opts.render(data);
            }
            toast(opts.successText || ('完成：' + summarize(data)), 'ok');
            if (opts.reload !== false) {
                setTimeout(() => location.reload(), 700);
            }
            return data;
        } catch (e) {
            toast(e.message, 'err');
            throw e;
        } finally {
            button.disabled = false;
            button.textContent = old;
        }
    }

    function summarize(data) {
        if (typeof data === 'number') {
            return data + ' 条数据';
        }
        if (typeof data === 'string') {
            return data;
        }
        return '成功';
    }

    /** 后端 LocalDateTime 序列化为 ISO 字符串，页面展示时去掉 T 与毫秒 */
    function dt(value) {
        if (!value) {
            return '—';
        }
        return String(value).replace('T', ' ').substring(0, 19);
    }

    function param(name) {
        return new URLSearchParams(location.search).get(name);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    /** 把接口返回写入 <pre> 面板，便于查看原始报文 */
    function fill(targetId, data) {
        const el = document.getElementById(targetId);
        if (!el) {
            return;
        }
        el.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
    }

    /**
     * 轻量折线图（自实现，不依赖外部图表库）
     * labels 横轴文本数组，values 数值数组
     */
    function lineChart(canvas, labels, values) {
        if (!canvas) {
            return;
        }
        const dpr = window.devicePixelRatio || 1;
        const width = canvas.clientWidth || 600;
        const height = canvas.clientHeight || 220;
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        const ctx = canvas.getContext('2d');
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        const pad = {left: 52, right: 16, top: 14, bottom: 26};
        const innerW = Math.max(width - pad.left - pad.right, 10);
        const innerH = Math.max(height - pad.top - pad.bottom, 10);
        const max = Math.max.apply(null, values.concat([0]));
        const min = Math.min.apply(null, values.concat([0]));
        const span = (max - min) || 1;

        // 网格与 Y 轴刻度
        ctx.strokeStyle = '#e3e8f0';
        ctx.fillStyle = '#8a97ab';
        ctx.font = '11px sans-serif';
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const y = pad.top + innerH * i / 4;
            ctx.beginPath();
            ctx.moveTo(pad.left, y);
            ctx.lineTo(pad.left + innerW, y);
            ctx.stroke();
            const v = max - span * i / 4;
            ctx.fillText(fmtNum(v), 4, y + 4);
        }

        if (values.length === 0) {
            ctx.fillStyle = '#8a97ab';
            ctx.fillText('暂无数据', pad.left + 8, pad.top + innerH / 2);
            return;
        }

        const stepX = values.length > 1 ? innerW / (values.length - 1) : 0;
        const point = (i) => ({
            x: pad.left + stepX * i,
            y: pad.top + innerH - ((values[i] - min) / span) * innerH
        });

        // 面积
        ctx.beginPath();
        ctx.moveTo(pad.left, pad.top + innerH);
        values.forEach((v, i) => {
            const p = point(i);
            ctx.lineTo(p.x, p.y);
        });
        ctx.lineTo(pad.left + stepX * (values.length - 1), pad.top + innerH);
        ctx.closePath();
        ctx.fillStyle = 'rgba(0,101,255,.12)';
        ctx.fill();

        // 折线
        ctx.beginPath();
        values.forEach((v, i) => {
            const p = point(i);
            if (i === 0) {
                ctx.moveTo(p.x, p.y);
            } else {
                ctx.lineTo(p.x, p.y);
            }
        });
        ctx.strokeStyle = '#0065ff';
        ctx.lineWidth = 2;
        ctx.stroke();

        // 数据点 + 稀疏横轴文本
        ctx.fillStyle = '#0065ff';
        values.forEach((v, i) => {
            const p = point(i);
            ctx.beginPath();
            ctx.arc(p.x, p.y, 2.5, 0, Math.PI * 2);
            ctx.fill();
        });
        ctx.fillStyle = '#8a97ab';
        const labelStep = Math.ceil(labels.length / 8) || 1;
        labels.forEach((text, i) => {
            if (i % labelStep !== 0 && i !== labels.length - 1) {
                return;
            }
            const p = point(i);
            ctx.fillText(String(text), p.x - 18, pad.top + innerH + 16);
        });
    }

    function fmtNum(v) {
        if (Math.abs(v) >= 1000000) {
            return (v / 1000000).toFixed(1) + 'M';
        }
        if (Math.abs(v) >= 1000) {
            return (v / 1000).toFixed(1) + 'k';
        }
        return (Math.round(v * 100) / 100).toString();
    }

    /** 下载 base64 内容（如 Ozon 返回的 PDF 标签） */
    function downloadBase64(base64, filename, mime) {
        const binary = atob(base64.replace(/\s/g, ''));
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        const blob = new Blob([bytes], {type: mime || 'application/pdf'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || 'label.pdf';
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 2000);
    }

    return {request, get, post, run, toast, dt, param, escapeHtml, fill, lineChart, downloadBase64};
})();
