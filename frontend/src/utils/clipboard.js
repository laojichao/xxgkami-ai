/**
 * Copies text to the clipboard with a fallback for non-secure contexts.
 * @param {string} text - The text to copy.
 * @returns {Promise<boolean>} - Resolves to true if successful, false otherwise.
 */
export async function copyToClipboard(text) {
  if (!text) return false;

  // Try the modern Clipboard API first
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch (err) {
      console.warn('Clipboard API failed, trying fallback...', err);
    }
  }

  // Fallback using document.execCommand('copy')
  const textArea = document.createElement("textarea");
  textArea.value = text;

  // Ensure the textarea is not visible but part of the DOM
  textArea.style.position = "fixed";
  textArea.style.left = "-9999px";
  textArea.style.top = "0";
  textArea.setAttribute('readonly', '');

  document.body.appendChild(textArea);

  try {
    textArea.focus();
    textArea.select();

    const successful = document.execCommand('copy');
    return successful;
  } catch (err) {
    console.error('Fallback copy failed:', err);
    return false;
  } finally {
    // 确保 textarea 无论成功与否都从 DOM 中移除，防止内存泄漏
    if (textArea.parentNode) {
      document.body.removeChild(textArea);
    }
  }
}
