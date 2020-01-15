import java.util.Arrays;

public class QuickSort {
  public static void main(String[] args) {
    int[] nums = {2019, 4, 1, 2, 40, 54, 117000};
    quickSort(nums, 0, nums.length - 1);
    System.out.println(Arrays.toString(nums));
  }

  private static void quickSort(int nums[], int left, int right) {
    if (left < right) {
      int pivot = partition(nums, left, right);
      quickSort(nums, left, pivot - 1);
      quickSort(nums, pivot + 1, right);
    }
  }

  private static int partition(int nums[], int left, int right) {
    int pivot = nums[right], hi = right;
    while (left < right) {
      if (nums[left] >= pivot) {
          swap(nums, left, --right);
      } else {
          left++;
      }
    }
    swap(nums, right, hi);
    return right;
  }

  private static void swap(int[] nums, int left, int right) {
    int temp = nums[left];
    nums[left] = nums[right];
    nums[right] = temp;
  }
}
