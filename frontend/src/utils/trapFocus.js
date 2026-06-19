/**
 * 焦点陷阱工具函数
 * 用于模态框组件，阻止 Tab 键将焦点移出模态框，提升无障碍访问体验
 */

/**
 * 阻止 Tab 键将焦点移出当前模态框
 * 在模态框的 keydown 事件中调用，Shift+Tab 循环到末尾时跳回首个可聚焦元素
 * @param {KeyboardEvent} event - 键盘事件对象
 */
export function trapFocus(event) {
    if (event.key !== 'Tab') return
    const modal = event.currentTarget
    if (!modal) return
    const focusable = modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
    if (focusable.length === 0) return
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey) {
        // Shift+Tab：从首个元素跳回末尾元素
        if (document.activeElement === first) {
            event.preventDefault()
            last.focus()
        }
    } else {
        // Tab：从末尾元素跳回首个元素
        if (document.activeElement === last) {
            event.preventDefault()
            first.focus()
        }
    }
}
