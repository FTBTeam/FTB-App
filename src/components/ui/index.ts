import ClosablePanel from './ClosablePanel.vue';
import Link from './Link.vue'
import Loader from './Loader.vue'
import Message from './Message.vue'
import Popover from './Popover.vue'
import ProgressBar from './ProgressBar.vue'
import Selection2 from './Selection2.vue'
import UiBadge from './UiBadge.vue'
import UiButton from './UiButton.vue'
import UiPagination from './UiPagination.vue'
import UiToggle from './UiToggle.vue'
import Modal from './modal/Modal.vue'
import ModalBody from './modal/ModalBody.vue'
import ModalFooter from './modal/ModalFooter.vue'
import FTBButton from './input/FTBButton.vue'
import FTBInput from './input/FTBInput.vue'
import FTBSearchBar from './input/FTBSearchBar.vue'
import FTBSlider from './input/FTBSlider.vue'
import Button from './Button/Button.vue'
import Input from './form/Input/Input.vue'
import InputNumber from './form/InputNumber/InputNumber.vue'

export {
  ClosablePanel,
  Link,
  Loader,
  /**
   * @deprecated Use Message instead
   */
  Message as UiMessage,
  Message,
  Popover,
  ProgressBar,
  Selection2,
  UiBadge,
  /**
   * @deprecated Use Button instead
   */
  UiButton,
  UiPagination,
  UiToggle,
  Modal,
  ModalBody,
  ModalFooter,
  /**
   * @deprecated Use Button instead
   */
  FTBButton,
  /**
   * @deprecated Use Input instead
   */
  FTBInput,
  /**
   * @deprecated Use Input instead
   */
  FTBSearchBar,
  FTBSlider,
  
  Button,
  Input,
  InputNumber,
}